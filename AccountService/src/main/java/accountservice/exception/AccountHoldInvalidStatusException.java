package accountservice.exception;

import enums.account.ReservationStatus;

public class AccountHoldInvalidStatusException extends RuntimeException {
    public AccountHoldInvalidStatusException(ReservationStatus status) {
        super("Illegal status of account hold " + status);
    }
}
