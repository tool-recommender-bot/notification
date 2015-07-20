package com.smoketurner.notification.application.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.ExecutionException;
import org.junit.Ignore;
import org.junit.Test;
import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.UpdateValue;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;

public class CursorStoreTest {

    private static final String TEST_USER = "test";
    private static final String CURSOR_NAME = "notifications";
    private final MetricRegistry registry = new MetricRegistry();
    private final RiakClient client = mock(RiakClient.class);
    private final CursorStore store = new CursorStore(registry, client);

    @Test
    @Ignore
    public void testFetch() throws Exception {
        final FetchValue.Response response = mock(FetchValue.Response.class);

        final Optional<Long> expected = Optional.of(1L);

        when(client.execute(any(FetchValue.class))).thenReturn(response);

        final Optional<Long> actual = store.fetch(TEST_USER, CURSOR_NAME);
        verify(client).execute(any(FetchValue.class));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testStore() throws Exception {
        store.store(TEST_USER, CURSOR_NAME, 1L);
        verify(client).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreNullUsername() throws Exception {
        try {
            store.store(null, CURSOR_NAME, 1L);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
        verify(client, never()).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreEmptyUsername() throws Exception {
        try {
            store.store("", CURSOR_NAME, 1L);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreNullCursorName() throws Exception {
        try {
            store.store(TEST_USER, null, 1L);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
        verify(client, never()).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreEmptyCursorName() throws Exception {
        try {
            store.store(TEST_USER, "", 1L);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreExecutionException() throws Exception {
        when(client.execute(any(UpdateValue.class))).thenThrow(
                new ExecutionException(new Exception()));

        try {
            store.store(TEST_USER, CURSOR_NAME, 1L);
            failBecauseExceptionWasNotThrown(NotificationStoreException.class);
        } catch (NotificationStoreException e) {
        }

        verify(client).execute(any(UpdateValue.class));
    }

    @Test
    public void testStoreInterruptedException() throws Exception {
        when(client.execute(any(UpdateValue.class))).thenThrow(
                new InterruptedException());

        try {
            store.store(TEST_USER, CURSOR_NAME, 1L);
            failBecauseExceptionWasNotThrown(NotificationStoreException.class);
        } catch (NotificationStoreException e) {
        }

        verify(client).execute(any(UpdateValue.class));
    }

    @Test
    public void testDelete() throws Exception {
        store.delete(TEST_USER, CURSOR_NAME);
        verify(client).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteNullUsername() throws Exception {
        try {
            store.delete(null, CURSOR_NAME);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
        verify(client, never()).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteEmptyUsername() throws Exception {
        try {
            store.delete("", CURSOR_NAME);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteNullCursorName() throws Exception {
        try {
            store.delete(TEST_USER, null);
            failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
        }
        verify(client, never()).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteEmptyCursorName() throws Exception {
        try {
            store.delete(TEST_USER, "");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
        }
        verify(client, never()).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteExecutionException() throws Exception {
        when(client.execute(any(DeleteValue.class))).thenThrow(
                new ExecutionException(new Exception()));

        try {
            store.delete(TEST_USER, CURSOR_NAME);
            failBecauseExceptionWasNotThrown(NotificationStoreException.class);
        } catch (NotificationStoreException e) {
        }

        verify(client).execute(any(DeleteValue.class));
    }

    @Test
    public void testDeleteInterruptedException() throws Exception {
        when(client.execute(any(DeleteValue.class))).thenThrow(
                new InterruptedException());

        try {
            store.delete(TEST_USER, CURSOR_NAME);
            failBecauseExceptionWasNotThrown(NotificationStoreException.class);
        } catch (NotificationStoreException e) {
        }

        verify(client).execute(any(DeleteValue.class));
    }

    @Test
    public void testGetCursorKey() {
        assertThat(store.getCursorKey(TEST_USER, CURSOR_NAME)).isEqualTo(
                "test-notifications");
    }
}