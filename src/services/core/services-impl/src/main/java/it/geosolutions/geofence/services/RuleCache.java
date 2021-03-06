package it.geosolutions.geofence.services;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.googlecode.genericdao.search.Search;
import it.geosolutions.geofence.core.dao.RuleDAO;
import it.geosolutions.geofence.core.model.Rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cache rules for a short time so that the DAO doesn't need to be hit for each query.
 * <
 * Created by Jesse on 3/20/2014.
 */
public class RuleCache {
    private final MetricRegistry metricRegistry;

    public List<Rule> findAll() {
        return search(null);
    }

    private static class Entry {
        long lastUpdate;
        List<Rule> rules;
    }

    private final RuleDAO ruleDAO;
    private final Map<Search, Entry> cache = new HashMap<Search, Entry>(10000);
    public RuleCache(RuleDAO ruleDAO, MetricRegistry metricRegistry) {
        this.ruleDAO = ruleDAO;
        this.metricRegistry = metricRegistry;
    }

    public synchronized List<Rule> search(Search search) {
        Entry entry = cache.get(search);
        final long queryTime = System.currentTimeMillis();
        if (entry == null || queryTime - entry.lastUpdate > TimeUnit.SECONDS.toMillis(30)) {
            this.metricRegistry.meter("RuleCache.refreshCache()").mark();
            final List<Rule> results;
            if (search == null) {
                results = this.ruleDAO.findAll();
            } else {
                results = this.ruleDAO.search(search);
            }
            if (entry == null) {
                entry = new Entry();
                cache.put(search, entry);
            }
            entry.lastUpdate = queryTime;
            entry.rules = results;
        }
        return entry.rules;
    }
}
