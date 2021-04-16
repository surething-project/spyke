package spyke.engine.iptables.model;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import spyke.engine.iptables.model.types.Filter;
import spyke.engine.iptables.model.types.Protocol;
import spyke.engine.iptables.model.types.Table;

/**
 * Iptables rule.
 */
public class Rule {

    /**
     * The optional input interface.
     */
    private final Optional<String> in;
    /**
     * The optional output interface.
     */
    private final Optional<String> out;
    /**
     * The optional source domain or ip address.
     */
    private final Optional<String> source;
    /**
     * The optional destination domain or ip address.
     */
    private final Optional<String> destination;
    /**
     * The optional {@link Protocol}.
     */
    private final Optional<Protocol> protocol;
    /**
     * The optional mark. Only available with {@link Table#MANGLE}.
     */
    private final Optional<String> mark;
    /**
     * The optional {@link Table}.
     */
    private final Optional<Table> table;
    /**
     * The optional quota.
     */
    private final Optional<String> quota;
    /**
     * The optional hashlimit.
     */
    private final Optional<String> hashlimit;
    /**
     * The optional hashlimit name.
     */
    private final Optional<String> hashlimitName;
    /**
     * The optional limit.
     */
    private final Optional<String> limit;
    /**
     * The optional {@link Filter}.
     */
    private final Filter filter;

    private Rule(final Builder builder) {
        Preconditions.checkNotNull(builder.filter, "Filter can not be null.");
        this.in = builder.in;
        this.out = builder.out;
        this.source = builder.source;
        this.destination = builder.destination;
        this.protocol = builder.protocol;
        this.filter = builder.filter;
        this.mark = builder.mark;
        this.table = builder.table;
        this.quota = builder.quota;
        this.hashlimit = builder.hashlimit;
        this.hashlimitName = builder.hashlimitName;
        this.limit = builder.limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getIn() {
        return this.in;
    }

    public Optional<String> getOut() {
        return this.out;
    }

    public Optional<Table> getTable() {
        return this.table;
    }

    public Optional<String> getSource() {
        return this.source;
    }

    public Optional<String> getDestination() {
        return this.destination;
    }

    public Optional<Protocol> getProtocol() {
        return this.protocol;
    }

    public Filter getFilter() {
        return this.filter;
    }

    public Optional<String> getMark() {
        return this.mark;
    }

    public Optional<String> getQuota() {
        return this.in;
    }

    public Optional<String> getHashlimit() {
        return this.hashlimit;
    }

    public Optional<String> getHashlimitName() {
        return this.hashlimitName;
    }

    public Optional<String> getLimit() {
        return this.limit;
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(
                this.in,
                this.out,
                this.source,
                this.destination,
                this.protocol,
                this.filter,
                this.mark,
                this.table,
                this.quota,
                this.hashlimit,
                this.hashlimitName,
                this.limit
        );
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Rule other = (Rule) obj;
        return Objects.equal(this.in, other.in) && Objects.equal(this.out, other.out);
    }

    @Override
    public String toString() {

        String rule = " ";

        if (this.table.isPresent())
            rule += "-t " + this.table.get() + " ";

        if (this.in.isPresent())
            rule += "-i " + this.in.get() + " ";

        if (this.out.isPresent())
            rule += "-o " + this.out.get() + " ";

        if (this.source.isPresent()) {
            rule += "-s " + this.source.get() + " ";
        }

        if (this.destination.isPresent()) {
            rule += "-d " + this.destination.get() + " ";
        }

        if (this.protocol.isPresent())
            rule += "-p " + this.protocol.get() + " ";

        if (this.hashlimit.isPresent() && this.hashlimitName.isPresent())
            rule += "-m hashlimit --hashlimit-name " + this.hashlimitName.get() + " --hashlimit-above " + this.hashlimit.get() + " ";

        if (this.quota.isPresent())
            rule += "-m quota --quota " + this.quota.get() + " ";

        if (this.limit.isPresent()) {
            rule += "-m limit --limit " + this.limit.get() + "/m ";
        }

        rule += "-j " + this.filter;

        return rule;
    }

    /**
     * The builder for {@link Rule} instance.
     */
    public static class Builder {

        /**
         * The optional input interface.
         */
        private Optional<String> in = Optional.absent();
        /**
         * The optional output interface.
         */
        private Optional<String> out = Optional.absent();
        /**
         * The optional source domain or ip address.
         */
        private Optional<String> source = Optional.absent();
        /**
         * The optional destination domain or ip address.
         */
        private Optional<String> destination = Optional.absent();
        /**
         * The optional {@link Protocol}.
         */
        private Optional<Protocol> protocol = Optional.absent();
        /**
         * The {@link Filter}.
         */
        private Filter filter;
        /**
         * The optional mark. Only available with {@link Table#MANGLE}.
         */
        private Optional<String> mark = Optional.absent();
        /**
         * The optional {@link Table}.
         */
        private Optional<Table> table = Optional.absent();
        /**
         * The optional quota.
         */
        private Optional<String> quota = Optional.absent();
        /**
         * The optional hashlimit.
         */
        private Optional<String> hashlimit = Optional.absent();
        /**
         * The optional hashlimit name.
         */
        private Optional<String> hashlimitName = Optional.absent();
        /**
         * The optional limit.
         */
        private Optional<String> limit = Optional.absent();

        /**
         * Creates a new instance of {@link Rule}.
         */
        private Builder() {
        }

        public Builder in(final String in) {
            this.in = Optional.of(in);
            return this;
        }

        public Builder out(final String out) {
            this.out = Optional.of(out);
            return this;
        }

        public Builder source(final String source) {
            this.source = Optional.of(source);
            return this;
        }

        public Builder destination(final String destination) {
            this.destination = Optional.of(destination);
            return this;
        }

        public Builder protocol(final Protocol protocol) {
            this.protocol = Optional.of(protocol);
            return this;
        }

        public Builder filter(final Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder mark(final String mark) {
            this.mark = Optional.of(mark);
            return this;
        }

        public Builder table(final Table table) {
            this.table = Optional.of(table);
            return this;
        }

        public Builder quota(final String quota) {
            this.quota = Optional.of(quota);
            return this;
        }

        public Builder hashlimit(final String hashlimit) {
            this.hashlimit = Optional.of(hashlimit);
            return this;
        }

        public Builder hashlimitName(final String hashlimitName) {
            this.hashlimitName = Optional.of(hashlimitName);
            return this;
        }

        public Builder limit(final String limit) {
            this.limit = Optional.of(limit);
            return this;
        }

        public Builder from(final Rule rule) {
            this.in = rule.getIn();
            this.out = rule.getOut();
            this.source = rule.getSource();
            this.destination = rule.getDestination();
            this.protocol = rule.getProtocol();
            this.filter = rule.getFilter();
            this.mark = rule.getMark();
            this.table = rule.getTable();
            this.quota = rule.getQuota();
            this.hashlimit = rule.getHashlimit();
            this.hashlimitName = rule.getHashlimitName();
            this.limit = rule.getLimit();
            return this;
        }

        public Rule build() {
            return new Rule(this);
        }
    }
}

