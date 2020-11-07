package General;

import java.util.Objects;

public class Term implements Comparable{
    String name;
    int df, count;
    boolean cap;
    boolean junk;

    public Term(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setJunk(boolean junk){this.junk=junk;}

    public boolean getJunk(){
        return junk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Term term = (Term) o;
        return name.toLowerCase().equals(term.name.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    public int getDf() {
        return df;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public void setCap(boolean cap) {
        this.cap = cap;
    }

    public String getName() {
        return name.toLowerCase();
    }

    public boolean isCap() {
        return cap;
    }

    @Override
    public String toString() {
        if(cap)
            return name.toUpperCase()+", "+count;
        return name.toLowerCase()+", "+count;
    }

    @Override
    public int compareTo(Object o) {
        if (this == o) return 0;
        if (o == null || getClass() != o.getClass()) return 1;
        Term term = (Term) o;
        return name.toLowerCase().compareTo(term.name.toLowerCase());
    }
}
