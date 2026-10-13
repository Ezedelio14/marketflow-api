package com.marketflow.api.domain.common.exception;

/**
 * Raised by the order state machine when a transition is not allowed from the current state.
 */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String from, String action) {
        super("INVALID_STATE_TRANSITION", "Action '%s' is not allowed when order is %s".formatted(action, from));
    }
}
