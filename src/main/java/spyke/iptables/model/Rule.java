package spyke.iptables.model;

import spyke.iptables.variable.Filter;
import spyke.iptables.variable.Table;

import java.util.List;

public class Rule {
    private String in;
    private String out;
    private String source;
    private String destination;
    private String protocol;
    private Filter filter;
    private String mark;
    private Table table;
    private String quota;
    private String hashlimit;
    private String hashlimit_name;

    private String limit;

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getMark() { return mark; }

    public void setMark( String mark ) { this.mark=mark; }

    public String getQuota() {
        return in;
    }

    public void setQuota(String quota) {
        this.quota = quota;
    }

    public String getHashlimit() {
        return hashlimit;
    }

    public void setHashlimit(String hashlimit) {
        this.hashlimit = hashlimit;
    }

    public String getHashlimitName() {
        return hashlimit_name;
    }

    public void setHashlimitName(String hashlimit_name) {
        this.hashlimit_name = hashlimit_name;
    }

    public String getLimit(){
        return limit;
    }

    public void setLimit(String limit){
        this.limit = limit;
    }

    private <T> boolean deepEquals(List<T> lhs, List<T> rhs) {
        if (lhs == null && rhs == null)
            return true;

        if (lhs == null || rhs == null)
            return false;

        if (lhs.size() != rhs.size())
            return false;

        for (int i = 0; i < lhs.size(); i++) {
            T l = lhs.get(i);
            T r = rhs.get(i);

            if (l == null && r == null)
                continue;

            if (l == null || r == null)
                return false;

            if (!l.equals(r))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {

        String s = " ";

        if(table != null)
            s += "-t " + table + " ";

        if (in != null)
            s += "-i " + in + " ";

        if (out != null)
            s += "-o " + out + " ";

        if (source != null){
            s += "-s " + source + " ";
        }

        if (destination != null){
            s += "-d " + destination + " ";
        }

        if (protocol != null)
            s += "-p " + protocol + " ";

        if(hashlimit != null && hashlimit_name!=null)
            s += "-m hashlimit --hashlimit-name " + hashlimit_name + " --hashlimit-above " + hashlimit + " ";

        if(quota != null)
            s += "-m quota --quota " + quota + " ";

        if(limit != null){
            s += "-m limit --limit "+ limit +"/m ";
        }

        s += "-j " + filter;

        return s;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Rule other = (Rule) obj;
        if (in == null) {
            if (other.in != null)
                return false;
        } else if (!in.equals(other.in))
            return false;
        if (out == null) {
            if (other.out != null)
                return false;
        } else if (!out.equals(other.out))
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (destination == null) {
            if (other.destination != null)
                return false;
        } else if (!destination.equals(other.destination))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        if (table == null) {
            if (other.table != null)
                return false;
        } else if (!table.equals(other.table))
            return false;
        if (mark == null) {
            if (other.mark != null)
                return false;
        } else if (!mark.equals(other.mark))
            return false;
        if (quota == null) {
            if (other.quota != null)
                return false;
        } else if (!quota.equals(other.quota))
            return false;
        if (hashlimit == null) {
            if (other.hashlimit != null)
                return false;
        } else if (!hashlimit.equals(other.hashlimit))
            return false;
        if (limit == null) {
            if (other.limit != null)
                return false;
        } else if (!limit.equals(other.limit))
            return false;
        return true;
    }
    @Override
    public int hashCode(){
        if(this.filter == null || (this.source == null && this.destination == null)){
            return -1;
        }
        if(this.source != null)
            return (this.filter+source).hashCode();
        return (this.filter+destination).hashCode();
    }
}

