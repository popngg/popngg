import importlib.util
import pathlib
import unittest
from unittest.mock import MagicMock, patch


MODULE_PATH = pathlib.Path(__file__).parents[1] / 'monitoring' / 'incident-bot' / 'incident_bot.py'
SPEC = importlib.util.spec_from_file_location('incident_bot', MODULE_PATH)
incident_bot = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(incident_bot)


class IncidentBotTest(unittest.TestCase):
    def test_request_json_identifies_client_to_discord(self):
        response = MagicMock()
        response.read.return_value = b'{}'
        response.__enter__.return_value = response
        with patch.object(incident_bot.urllib.request, 'urlopen', return_value=response) as urlopen:
            incident_bot.request_json('https://discord.com/api/v10/users/@me')

        request = urlopen.call_args.args[0]
        self.assertEqual(incident_bot.USER_AGENT, request.get_header('User-agent'))

    def test_creates_parent_thread_and_initial_reply(self):
        responses = [
            {'channel_id': 'channel-1', 'id': 'message-1'},
            {'id': 'thread-1'},
            {},
        ]
        with patch.object(incident_bot, 'WEBHOOK', 'https://discord.example/webhook'), \
                patch.object(incident_bot, 'BOT_TOKEN', 'token'), \
                patch.object(incident_bot, 'request_json', side_effect=responses) as request:
            thread_id = incident_bot.open_thread('manual', 'test', 'detail', True)

        self.assertEqual('thread-1', thread_id)
        self.assertEqual(3, request.call_count)
        self.assertIn('wait=true', request.call_args_list[0].args[0])
        self.assertIn('/channels/channel-1/messages/message-1/threads', request.call_args_list[1].args[0])
        self.assertIn('thread_id=thread-1', request.call_args_list[2].args[0])

    def test_requires_discord_configuration(self):
        with patch.object(incident_bot, 'WEBHOOK', ''), patch.object(incident_bot, 'BOT_TOKEN', ''):
            with self.assertRaises(RuntimeError):
                incident_bot.open_thread('manual', 'test', 'detail', True)


if __name__ == '__main__':
    unittest.main()
