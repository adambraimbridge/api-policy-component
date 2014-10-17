package com.ft.up.apipolicy;

import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * An implementation of {@link javax.ws.rs.core.MultivaluedMap} where keys and values are
 * instances of String.
 *
 * Adapted from Jersey's class of a similar name.
 *
 * @author Simon.Gibbs
 * @author Paul.Sandoz@Sun.Com
 */
public class LinkedMultivalueMap
        extends LinkedHashMap<String, List<String>>
        implements MultivaluedMap<String, String> {

    static final long serialVersionUID = -6052320403766368902L;

    public LinkedMultivalueMap() { }

    public LinkedMultivalueMap(MultivaluedMap<String, String> that) {
        for (Map.Entry<String, List<String>> e : that.entrySet()) {
            this.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
    }

    // MultivaluedMap

    @Override
    public final void putSingle(String key, String value) {
        List<String> l = getList(key);

        l.clear();
        if (value != null)
            l.add(value);
        else
            l.add("");
    }

    @Override
    public final void add(String key, String value) {
        List<String> l = getList(key);

        if (value != null)
            l.add(value);
        else
            l.add("");
    }

    @Override
    public final String getFirst(String key) {
        List<String> values = get(key);
        if (values != null && values.size() > 0)
            return values.get(0);
        else
            return null;
    }

    //

    public final void addFirst(String key, String value) {
        List<String> l = getList(key);

        if (value != null)
            l.add(0, value);
        else
            l.add(0, "");
    }

    public final <A> List<A> get(String key, Class<A> type) {
        Constructor<A> c = null;
        try {
            c = type.getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(type.getName()+" has no String constructor", ex);
        }

        ArrayList<A> l = null;
        List<String> values = get(key);
        if (values != null) {
            l = new ArrayList<A>();
            for (String value: values) {
                try {
                    l.add(c.newInstance(value));
                } catch (Exception ex) {
                    l.add(null);
                }
            }
        }
        return l;
    }

    public final void putSingle(String key, Object value) {
        List<String> l = getList(key);

        l.clear();
        if (value != null)
            l.add(value.toString());
        else
            l.add("");
    }

    public final void add(String key, Object value) {
        List<String> l = getList(key);

        if (value != null)
            l.add(value.toString());
        else
            l.add("");
    }

    private List<String> getList(String key) {
        List<String> l = get(key);
        if (l == null) {
            l = new LinkedList<String>();
            put(key, l);
        }
        return l;
    }

    public final <A> A getFirst(String key, Class<A> type) {
        String value = getFirst(key);
        if (value == null)
            return null;
        Constructor<A> c = null;
        try {
            c = type.getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(type.getName()+" has no String constructor", ex);
        }
        A retVal = null;
        try {
            retVal = c.newInstance(value);
        } catch (Exception ex) {
        }
        return retVal;
    }

    @SuppressWarnings("unchecked")
    public final <A> A getFirst(String key, A defaultValue) {
        String value = getFirst(key);
        if (value == null)
            return defaultValue;

        Class<A> type = (Class<A>)defaultValue.getClass();

        Constructor<A> c = null;
        try {
            c = type.getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(type.getName()+" has no String constructor", ex);
        }
        A retVal = defaultValue;
        try {
            retVal = c.newInstance(value);
        } catch (Exception ex) {
        }
        return retVal;
    }
}

