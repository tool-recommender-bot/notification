/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.smoketurner.notification.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.smoketurner.notification.api.Notification;

public class NotificationClient implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationClient.class);
  private static final String APPLICATION_JSON = "application/json";
  private final Client client;
  private final URI destination;

  /**
   * Constructor
   *
   * @param client Jersey Client
   * @param destination API endpoint
   */
  public NotificationClient(@Nonnull final Client client, @Nonnull final URI destination) {
    this.client = Preconditions.checkNotNull(client);
    this.destination = Preconditions.checkNotNull(destination);
  }

  /**
   * Fetch all notifications for a given username. This method will paginate through all of the
   * available notifications for a user.
   *
   * @param username User to fetch notifications
   * @return Sorted set of all notifications for the user
   */
  public ImmutableSortedSet<Notification> fetch(@Nonnull final String username) {
    final URI uri = getTarget(username);

    final ImmutableSortedSet.Builder<Notification> results = ImmutableSortedSet.naturalOrder();

    String nextRange = null;
    boolean paginate = true;
    while (paginate) {
      LOGGER.info("GET {}", uri);
      final Invocation.Builder builder = client.target(uri).request(APPLICATION_JSON);
      if (nextRange != null) {
        builder.header("Range", nextRange);
      }

      final Response response = builder.get();
      nextRange = response.getHeaderString("Next-Range");
      if (nextRange == null) {
        paginate = false;
      }

      if (response.getStatus() == 200 || response.getStatus() == 206) {
        results.addAll(response.readEntity(new GenericType<List<Notification>>() {}));
      }
      response.close();
    }
    return results.build();
  }

  /**
   * Store a new notification for a user
   *
   * @param username User to add the notification
   * @param notification Notification to store
   * @return the newly stored notification
   */
  public Notification store(@Nonnull final String username, @Nonnull final Notification notification) {
    Preconditions.checkNotNull(notification);
    final URI uri = getTarget(username);
    LOGGER.debug("POST {}", uri);
    return client.target(uri).request(APPLICATION_JSON)
        .post(Entity.json(notification), Notification.class);
  }

  /**
   * Delete individual notification IDs for a given user.
   *
   * @param username User to delete notifications from
   * @param ids Notification IDs to delete
   */
  public void delete(@Nonnull final String username, @Nonnull final Collection<Long> ids) {
    Preconditions.checkNotNull(ids);
    Preconditions.checkArgument(!ids.isEmpty(), "ids cannot be empty");
    final URI uri =
        UriBuilder.fromUri(getTarget(username)).queryParam("ids", Joiner.on(",").join(ids)).build();
    LOGGER.debug("DELETE {}", uri);
    client.target(uri).request().delete();
  }

  /**
   * Delete all notifications for a given user.
   *
   * @param username User to delete notifications from
   */
  public void delete(@Nonnull final String username) {
    final URI uri = getTarget(username);
    LOGGER.debug("DELETE {}", uri);
    client.target(uri).request().delete();
  }

  /**
   * Return the ping response
   *
   * @return true if the ping response was successful, otherwise false
   */
  public boolean ping() {
    final URI uri = UriBuilder.fromUri(destination).path("/ping").build();
    LOGGER.debug("GET {}", uri);
    final String response = client.target(uri).request().get(String.class);
    return "pong".equals(response);
  }

  /**
   * Return the service version
   *
   * @return service version
   */
  public String version() {
    final URI uri = UriBuilder.fromUri(destination).path("/version").build();
    LOGGER.debug("GET {}", uri);
    return client.target(uri).request().get(String.class);
  }

  private URI getTarget(@Nonnull final String username) {
    Preconditions.checkNotNull(username);
    Preconditions.checkArgument(!username.isEmpty(), "username cannot be empty");
    return UriBuilder.fromUri(destination).path("/v1/notifications/{username}").build(username);
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}