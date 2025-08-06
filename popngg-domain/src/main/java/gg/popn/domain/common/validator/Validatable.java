package gg.popn.domain.common.validator;

import java.util.function.Supplier;

public interface Validatable {
    void validate();

    static <T extends Validatable> T createAndValidate(Supplier<T> supplier) {
        T instance = supplier.get();

        if (instance == null) {
            return null;
        }

        instance.validate();
        return instance;
    }
}
