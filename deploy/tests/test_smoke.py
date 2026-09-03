"""Exercise the real smoke script against a disposable loopback HTTP server."""
import http.server
import os
from pathlib import Path
import shutil
import subprocess
import threading
import time
import unittest


class SmokeTest(unittest.TestCase):
    def run_smoke(self, mode):
        requests = []

        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, *_args):
                pass

            def do_GET(self):
                requests.append(self.path)
                status = 200
                if self.path.startswith('/api/v1/users?'):
                    if mode == 'failure':
                        status = 503
                    elif mode == 'timeout':
                        time.sleep(11)
                self.send_response(status)
                self.end_headers()
                try:
                    self.wfile.write(b'{"status":"UP"}' if self.path == '/health'
                                     else b'private-response-sentinel')
                except (BrokenPipeError, ConnectionResetError):
                    pass

        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        try:
            env = os.environ.copy()
            env['BASE_URL'] = f'http://127.0.0.1:{server.server_port}'
            env.pop('SMOKE_POPTOMO_ID', None)
            env.pop('SMOKE_LOGIN_PASSWORD', None)
            bash = env.get('SMOKE_TEST_BASH') or shutil.which('bash')
            self.assertIsNotNone(bash, 'bash is required')
            result = subprocess.run([bash, 'deploy/bin/smoke-test.sh'], env=env,
                                    cwd=Path(__file__).resolve().parents[2],
                                    capture_output=True, text=True, timeout=25)
            self.assertNotIn('private-response-sentinel', result.stdout + result.stderr)
            return result, requests
        finally:
            server.shutdown()
            server.server_close()
            worker.join(timeout=2)

    def test_success_logs_timings_and_checks_first_and_repeat(self):
        result, requests = self.run_smoke('success')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(sum(path.startswith('/api/v1/users?') for path in requests), 2)
        self.assertRegex(result.stdout, r'request=users_clear_level_first http=200 first_byte=[0-9.]+s total=[0-9.]+s curl_exit=0')
        self.assertIn('request=users_clear_level_repeat http=200', result.stdout)

    def test_failure_identifies_request_and_does_not_continue(self):
        result, requests = self.run_smoke('failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('request=users_clear_level_first http=503', result.stdout)
        self.assertIn('curl_exit=22', result.stdout)
        self.assertNotIn('users_clear_level_repeat', result.stdout)
        self.assertEqual(sum(path.startswith('/api/v1/users?') for path in requests), 1)

    def test_timeout_logs_curl_exit_28_without_relaxing_limit(self):
        result, _ = self.run_smoke('timeout')
        self.assertEqual(result.returncode, 28)
        self.assertIn('request=users_clear_level_first http=000', result.stdout)
        self.assertIn('curl_exit=28', result.stdout)


if __name__ == '__main__':
    unittest.main()
