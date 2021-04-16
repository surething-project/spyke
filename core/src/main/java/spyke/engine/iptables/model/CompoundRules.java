package spyke.engine.iptables.model;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import spyke.engine.iptables.model.types.Filter;

import java.util.List;

/**
 * Compound rules, an ordered Iptable rule.
 * <p>
 * <ul>
 *      <li>incoming with bandwidth, this is optional</li>
 *      <li>incoming with quota, this is optional</li>
 *      <li>incoming unlimited, this only exists if no quota exists</li>
 *      <li>outgoing with bandwidth, this is optional</li>
 *      <li>outgoing with quota, this is optional</li>
 *      <li>outgoing unlimited, this only exists if no quota exists</li>
 *      <li>incoming drop</li>
 *      <li>outgoing drop</li>
 * </ul>
 * There might be between 4 and 6 rules, because bandwidth is optional, and quota and unlimited are mutual exclusive.
 */
public class CompoundRules {

    /**
     * The optional first rule for incoming with bandwidth traffic.
     */
    private final Optional<Rule> first;
    /**
     * The optional second rule for incoming with quota traffic.
     */
    private final Optional<Rule> second;
    /**
     * The optional third rule for unlimited incoming traffic.
     */
    private final Optional<Rule> third;
    /**
     * The optional fourth rule for outgoing with bandwidth traffic.
     */
    private final Optional<Rule> fourth;
    /**
     * The optional fifth rule for outgoing with quota traffic.
     */
    private final Optional<Rule> fifth;
    /**
     * The optional sixth rule for unlimited outgoing traffic.
     */
    private final Optional<Rule> sixth;
    /**
     * The seventh for drop all incoming traffic.
     */
    private final Rule seventh;
    /**
     * The eighth for drop all outgoing traffic.
     */
    private final Rule eighth;

    private CompoundRules(final Builder builder) {
        this.first = builder.first;
        this.second = builder.second;
        this.third = builder.third;
        this.fourth = builder.fourth;
        this.fifth = builder.fifth;
        this.sixth = builder.sixth;
        this.seventh = builder.seventh;
        this.eighth = builder.eighth;
    }

    public Optional<Rule> getFirst() {
        return this.first;
    }

    public Optional<Rule> getSecond() {
        return this.second;
    }

    public Optional<Rule> getThird() {
        return this.third;
    }

    public Optional<Rule> getFourth() {
        return this.fourth;
    }

    public Optional<Rule> getFifth() {
        return this.fifth;
    }

    public Optional<Rule> getSixth() {
        return this.sixth;
    }

    public Rule getSeventh() {
        return this.seventh;
    }

    public Rule getEighth() {
        return this.eighth;
    }

    public BiMap<Integer, Rule> getBiMapRules() {
        final ImmutableBiMap.Builder biMap = ImmutableBiMap.builder();
        if (this.first.isPresent()) {
            biMap.put(1, this.first.get());
        }
        if (this.second.isPresent()) {
            biMap.put(2, this.second.get());
        }
        if (this.third.isPresent()) {
            biMap.put(3, this.third.get());
        }
        if (this.fourth.isPresent()) {
            biMap.put(4, this.fourth.get());
        }
        if (this.fifth.isPresent()) {
            biMap.put(5, this.fifth.get());
        }
        if (this.sixth.isPresent()) {
            biMap.put(6, this.sixth.get());
        }
        biMap.put(7, this.seventh);
        biMap.put(8, this.eighth);
        return biMap.build();
    }

    public List<Rule> getRules() {
        final ImmutableList.Builder list = ImmutableList.builder();
        if (this.first.isPresent()) {
            list.add(this.first.get());
        }
        if (this.second.isPresent()) {
            list.add(this.second.get());
        }
        if (this.third.isPresent()) {
            list.add(this.third.get());
        }
        if (this.fourth.isPresent()) {
            list.add(this.fourth.get());
        }
        if (this.fifth.isPresent()) {
            list.add(this.fifth.get());
        }
        if (this.sixth.isPresent()) {
            list.add(this.sixth.get());
        }
        list.add(this.seventh, this.eighth);
        return list.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for {@link CompoundRules} instance.
     */
    public static class Builder {

        /**
         * The optional first rule for incoming with bandwidth traffic.
         */
        private Optional<Rule> first = Optional.absent();
        /**
         * The optional second rule for incoming with quota traffic.
         */
        private Optional<Rule> second = Optional.absent();
        /**
         * The optional third rule for unlimited incoming traffic.
         */
        private Optional<Rule> third = Optional.absent();
        /**
         * The optional fourth rule for outgoing with bandwidth traffic.
         */
        private Optional<Rule> fourth = Optional.absent();
        /**
         * The optional fifth rule for outgoing with quota traffic.
         */
        private Optional<Rule> fifth = Optional.absent();
        /**
         * The optional sixth rule for unlimited outgoing traffic.
         */
        private Optional<Rule> sixth = Optional.absent();
        /**
         * The seventh for drop all incoming traffic.
         */
        private Rule seventh;
        /**
         * The eighth for drop all outgoing traffic.
         */
        private Rule eighth;

        /**
         * Creates a new instance of {@link CompoundRules}.
         */
        private Builder() {
        }

        public Builder first(final Rule first) {
            this.first = Optional.of(first);
            return this;
        }

        public Builder second(final Rule second) {
            this.second = Optional.of(second);
            return this;
        }

        public Builder third(final Rule third) {
            this.third = Optional.of(third);
            return this;
        }

        public Builder fourth(final Rule fourth) {
            this.fourth = Optional.of(fourth);
            return this;
        }

        public Builder fifth(final Rule fifth) {
            this.fifth = Optional.of(fifth);
            return this;
        }

        public Builder sixth(final Rule sixth) {
            this.sixth = Optional.of(sixth);
            return this;
        }

        public Builder seventh(final Rule seventh) {
            this.seventh = seventh;
            return this;
        }

        public Builder eighth(final Rule eighth) {
            this.eighth = eighth;
            return this;
        }

        public Builder defaultDrop(final String ip) {

            final Rule sourceRule = Rule.builder().source(ip).filter(Filter.DROP).build();
            final Rule destinationRule = Rule.builder().destination(ip).filter(Filter.DROP).build();
            // 7-outgoing drop
            this.seventh = sourceRule;
            // 8-incoming drop
            this.eighth = destinationRule;
            return this;
        }

        public Builder from(final CompoundRules rules) {
            this.first = rules.getFirst();
            this.second = rules.getSecond();
            this.third = rules.getThird();
            this.fourth = rules.getFourth();
            this.fifth = rules.getFifth();
            this.sixth = rules.getSixth();
            this.seventh = rules.getSeventh();
            this.eighth = rules.getEighth();
            return this;
        }

        public CompoundRules build() {
            return new CompoundRules(this);
        }
    }
}
