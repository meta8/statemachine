/*
 * (C) Copyright 2015 Meta8 SARL (http://meta8.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   J. R.
 */

package net.meta8.common.fsm.machine;

import static net.meta8.common.fsm.util.RichOptional.*;

import net.meta8.common.fsm.configuration.StateConfiguration;
import net.meta8.common.fsm.configuration.StateMachineConfiguration;
import net.meta8.common.fsm.configuration.StateMachineConfigurationDSL;
import net.meta8.common.fsm.exception.MissingStateConfigurationException;
import net.meta8.common.fsm.exception.UnknownTriggerException;
import net.meta8.common.fsm.state.States;
import net.meta8.common.fsm.transition.TimeoutTransition;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public final class StateMachine<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {
  private final @NonNull Optional<TGlobalContext>  globalContext;
  private final @NonNull Optional<TLocalContext>[] localContexts; // one initialContext per state

  private final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] machineConfiguration;

  private final ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);
  private @Nullable Future<?> pendingScheduledTimeoutTransition;

  private TState                                                            currentState;
  private StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> currentStateConfiguration;

  private AtomicInteger sequenceIndex = new AtomicInteger(-1);

  @SuppressWarnings("unchecked")
  public StateMachine(final @NonNull Class<TState>                                                               stateClazz,
                      final @NonNull StateMachineConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> configurationDsl) throws MissingStateConfigurationException {

    final StateMachineConfiguration<TState, TEvent, TLocalContext, TGlobalContext> configuration = (StateMachineConfiguration<TState, TEvent, TLocalContext, TGlobalContext>) configurationDsl;

    globalContext = configuration.cloneContext();
    localContexts = (Optional<TLocalContext>[]) Array.newInstance(Optional.class, stateClazz.getEnumConstants().length); // ugly, need to improve

    machineConfiguration = configuration.getStateMachineConfigurationClone();

    final @NonNull TState initialState = configuration.getInitialState();

    // check that initial state has a known configuration
    if (machineConfiguration[initialState.ordinal()] == null) {
      throw (new MissingStateConfigurationException(initialState.name()));
    }

    // for each state set a fresh copy of its internal state
    for (final TState state : stateClazz.getEnumConstants()) {
      if (machineConfiguration[state.ordinal()] != null) {
        localContexts[state.ordinal()] = machineConfiguration[state.ordinal()].cloneContext();
      }
    }

    // set initial state
    currentState = initialState;
    currentStateConfiguration = machineConfiguration[currentState.ordinal()];

    // trigger onEntry (if any) on currentStateConfiguration
    currentStateConfiguration.performInitialEntryActions(localContexts[currentState.ordinal()], globalContext);

    startTimeoutTransition(currentStateConfiguration);
  }

  public @NonNull TState getCurrentState() {
    return(currentState);
  }

  // synchronized is required to prevent timeout transition to mess up with normal event if both occur exactly in the same time
  // synchronized does not add a significant overhead in this case as contention is almost non existant
  public @NonNull synchronized TState fire(final @Nullable TEvent trigger) throws UnknownTriggerException, MissingStateConfigurationException {
    currentState              = currentStateConfiguration.fire(trigger, sequenceIndex, localContexts, globalContext, machineConfiguration);
    currentStateConfiguration = machineConfiguration[currentState.ordinal()];
    cleanFormerTimeoutTransition();
    startTimeoutTransition(currentStateConfiguration);
    return(currentState);
  }

  private void cleanFormerTimeoutTransition() {
    if (pendingScheduledTimeoutTransition != null) {
      if (pendingScheduledTimeoutTransition.isDone()) {
        System.out.println("Done");
        pendingScheduledTimeoutTransition = null;
      }
      else {
        System.out.println("Try cancelling...");
        if (pendingScheduledTimeoutTransition.cancel(true)) {
          System.out.println("Cancelled");
          pendingScheduledTimeoutTransition = null;
        }
        else {
          System.out.println("Unable to cancel");
          return; // can not cancel previous timeout transition !
        }
      }
    }
  }

  private void startTimeoutTransition(final StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stateConfiguration) {
    final Optional<TimeoutTransition<TState, TEvent, TLocalContext, TGlobalContext>> timeoutTransition = stateConfiguration.timeoutTransition();

    ifPresent(timeoutTransition,
              // SOME
              (someTransition) -> {
                System.out.println("Scheduling... "+timer);
                pendingScheduledTimeoutTransition = timer.schedule((Runnable) () -> {
                                                                     try {
                                                                       System.out.println("timeout triggered at "+System.currentTimeMillis()+" "+this);
                                                                       currentState              = currentStateConfiguration.fire(null, sequenceIndex, localContexts, globalContext, machineConfiguration);
                                                                       currentStateConfiguration = machineConfiguration[currentState.ordinal()];
                                                                       startTimeoutTransition(currentStateConfiguration);
                                                                     }
                                                                     catch (final UnknownTriggerException e) {
                                                                       // can never happened
                                                                     }
                                                                     catch (final MissingStateConfigurationException missingStateConfiguration) {
                                                                       //TODO error log
                                                                     }
                                                                   },
                                                                   timeoutTransition.get().duration.toMillis(),
                                                                   TimeUnit.MILLISECONDS);
              })
      // NONE
      .orElse(() -> pendingScheduledTimeoutTransition = null);
  }

  public void close() {
    if (pendingScheduledTimeoutTransition != null) {
      if (! pendingScheduledTimeoutTransition.isDone()) {
        System.out.println("Try cancelling...");
        if (pendingScheduledTimeoutTransition.cancel(true)) {
          System.out.println("Cancelled");
          pendingScheduledTimeoutTransition = null;
        } else {
          System.out.println("Unable to cancel");
          return; // can not cancel previous timeout transition !
        }
      }
    }
  }
}
