/*
 * Copyright (C) 2012 The Guava Authors
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
package jcog;

import jcog.event.ListTopic;
import jcog.event.Topic;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

/**
 * Modifications of Guava's ServiceManager
 *
 * <p>
 * A manager for monitoring and controlling a set of {@linkplain Service services}. This class
 * provides methods for {@linkplain #startAsync() starting}, {@linkplain #stopAsync() stopping} and
 * {@linkplain #servicesByState inspecting} a collection of {@linkplain Service services}.
 * Additionally, users can monitor state transitions with the {@linkplain Listener listener}
 * mechanism.
 * <p>
 * <p>While it is recommended that service lifecycles be managed via this class, state transitions
 * initiated via other mechanisms do not impact the correctness of its methods. For example, if the
 * services are started by some mechanism besides {@link #startAsync}, the listeners will be invoked
 * when appropriate and {@link #awaitHealthy} will still work as expected.
 * <p>
 * <p>Here is a simple example of how to use a {@code ServiceManager} to start a server.
 * <pre>   {@code
 * class Server {
 *   public static void main(String[] args) {
 *     Set<Service> services = ...;
 *     ServiceManager manager = new ServiceManager(services);
 *     manager.addListener(new Listener() {
 *         public void stopped() {}
 *         public void healthy() {
 *           // Services have been initialized and are healthy, start accepting requests...
 *         }
 *         public void failure(Service service) {
 *           // Something failed, at this point we could log it, notify a load balancer, or take
 *           // some other action.  For now we will just exit.
 *           System.exit(1);
 *         }
 *       },
 *       MoreExecutors.directExecutor());
 *
 *     Runtime.getRuntime().addShutdownHook(new Thread() {
 *       public void run() {
 *         // Give the services 5 seconds to stop to ensure that we are responsive to shutdown
 *         // requests.
 *         try {
 *           manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
 *         } catch (TimeoutException timeout) {
 *           // stopping timed out
 *         }
 *       }
 *     });
 *     manager.startAsync();  // start all the services asynchronously
 *   }
 * }}</pre>
 * <p>
 * <p>This class uses the ServiceManager's methods to start all of its services, to respond to
 * service failure and to ensure that when the JVM is shutting down all the services are stopped.
 *
 * @author Luke Sandberg (original)
 */
public class Services<C /* context */, K /* service key */, S extends Service>  {

    final Logger logger;
    public final C id;
    private final Executor exe;
    public final Topic<ObjectBooleanPair<? extends S>> change = new ListTopic<>();
    private final ConcurrentMap<K, S> services;

    enum ServiceState {
        Off {
            @Override public String toString() { return "-"; }
        },
        OffToOn {
            @Override public String toString() { return "-+"; }
        },
        On {
            @Override public String toString() { return "+"; }
        },
        OnToOff {
            @Override public String toString() { return "+-"; }
        },
        Deleted {
            @Override public String toString() { return "."; }
        }
    }


    public Services(C id) {
        this(id, ForkJoinPool.commonPool());
    }

    /**
     * Constructs a new instance for managing the given services.
     *
     * @param services The services to manage
     * @param x
     * @throws IllegalArgumentException if not all services are {@linkplain ServiceState#NEW new} or if there
     *                                  are any duplicate services.
     */
    public Services(@Nullable C id, Executor exe) {
        this.id = id == null ? (C)this : id;
        this.logger = LoggerFactory.getLogger(id.toString());
        this.exe = exe;
        this.services = new ConcurrentHashMap<>(64);
    }

    public Stream<S> stream() {
        return services.values().stream();
    }

    public Set<Map.Entry<K, S>> entrySet() {
        return services.entrySet();
    }

    public void add(K key, S s) {
        add(key, s, true);
    }

    public void add(K key, S s, boolean start) {
        S removed = services.put(key, s);

        if (removed == s)
            return; //no change

        if (removed != null) {
            //if start, then start after the previous stopped
            removed.stop(this, exe, start ? ()-> s.start(this, exe) : null);
        } else {
            if (start) {
                s.start(this, exe);
            }
        }
    }

    public void remove(K serviceID) {
        S s = services.get(serviceID);
        if (s!=null) {
            s.stop(this, exe, ()-> services.remove(serviceID));
        } else {
            logger.error("can not remove unknown service: {}", serviceID);
        }
    }

    /**
     * Initiates service {@linkplain Service#stopAsync shutdown} if necessary on all the services
     * being managed.
     *
     * @return this
     */
    public Services<C, K, S> stop() {
        for (S service : services.values()) {
            service.stop(this, exe, null);
        }
        return this;
    }

    public int size() {
        return services.size();
    }


    public void print(PrintStream out) {
        services.forEach((k, s) -> out.println(k + " " + s.get()));
    }


}