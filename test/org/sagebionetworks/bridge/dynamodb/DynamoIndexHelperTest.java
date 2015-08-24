package org.sagebionetworks.bridge.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DynamoIndexHelperTest {
    // test class to be used solely for mock testing
    public static class Thing {
        final String key;
        final String value;

        // needed for internal JSON conversion
        public Thing(@JsonProperty("key") String key) {
            this.key = key;
            value = null;
        }

        public Thing(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    // index.query() can't be mocked, so override queryHelper to sidestep this problem
    private static class TestDynamoIndexHelper extends DynamoIndexHelper {
        private final String expectedKey;
        private final String expectedValue;
        private final Iterable<Item> itemIterable;

        TestDynamoIndexHelper(String expectedKey, String expectedValue, Iterable<Item> itemIterable) {
            this.expectedKey = expectedKey;
            this.expectedValue = expectedValue;
            this.itemIterable = itemIterable;
        }

        @Override
        protected Iterable<Item> queryHelper(@Nonnull String indexKeyName, @Nonnull Object indexKeyValue) {
            assertEquals(expectedKey, indexKeyName);
            assertEquals(expectedValue, indexKeyValue);
            return itemIterable;
        }
    }

    @Test
    public void test() {
        // mock index
        List<Item> mockItemList = ImmutableList.of(new Item().with("key", "foo key"),
                new Item().with("key", "bar key"), new Item().with("key", "asdf key"),
                new Item().with("key", "jkl; key"));
        DynamoIndexHelper helper = new TestDynamoIndexHelper("test key", "test value", mockItemList);

        // mock mapper result
        Map<String, List<Object>> mockMapperResultMap = new HashMap<>();
        mockMapperResultMap.put("dummy key 1", ImmutableList.<Object>of(new Thing("foo key", "foo value"),
                new Thing("bar key", "bar value")));
        mockMapperResultMap.put("dummy key 2", ImmutableList.<Object>of(new Thing("asdf key", "asdf value"),
                new Thing("jkl; key", "jkl; value")));

        // mock mapper
        DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
        ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
        when(mockMapper.batchLoad(arg.capture())).thenReturn(mockMapperResultMap);
        helper.setMapper(mockMapper);

        // execute query keys and validate
        List<Thing> keyList = helper.queryKeys(Thing.class, "test key", "test value");
        validateKeyObjects(keyList);

        // execute
        List<Thing> resultList = helper.query(Thing.class, "test key", "test value");

        // Validate intermediate "key objects". This is a List<Object>, but because of type erasure, this should work,
        // at least in the test context.
        validateKeyObjects(arg.getValue());

        // Validate final results. Because of wonkiness with maps and ordering, we'll convert the Things into a map and
        // validate the map.
        assertEquals(4, resultList.size());
        Map<String, String> thingMap = new HashMap<>();
        for (Thing oneThing : resultList) {
            thingMap.put(oneThing.key, oneThing.value);
        }

        assertEquals(4, thingMap.size());
        assertEquals("foo value", thingMap.get("foo key"));
        assertEquals("bar value", thingMap.get("bar key"));
        assertEquals("asdf value", thingMap.get("asdf key"));
        assertEquals("jkl; value", thingMap.get("jkl; key"));
    }

    private static void validateKeyObjects(List<Thing> keyList) {
        assertEquals(4, keyList.size());

        assertEquals("foo key", keyList.get(0).key);
        assertNull(keyList.get(0).value);

        assertEquals("bar key", keyList.get(1).key);
        assertNull(keyList.get(1).value);

        assertEquals("asdf key", keyList.get(2).key);
        assertNull(keyList.get(2).value);

        assertEquals("jkl; key", keyList.get(3).key);
        assertNull(keyList.get(3).value);
    }
}
