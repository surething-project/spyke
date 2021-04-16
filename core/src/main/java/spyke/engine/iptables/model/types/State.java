package spyke.engine.iptables.model.types;

/**
 * The state of rule.
 */
public enum State {
    INVALID,
    ESTABLISHED,
    NEW,
    RELATED,
    UNTRACKED;
}
