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

package net.meta8.common.fsm.configuration;

import net.meta8.common.fsm.action.Action;
import net.meta8.common.fsm.action.ExecutableAction;
import net.meta8.common.fsm.action.Guard;
import net.meta8.common.fsm.exception.MissingStateConfigurationException;
import net.meta8.common.fsm.exception.StateConfigurationError;
import net.meta8.common.fsm.exception.UnknownTriggerException;
import net.meta8.common.fsm.state.States;
import net.meta8.common.fsm.transition.InitialTransition;
import net.meta8.common.fsm.transition.TimeoutTransition;
import net.meta8.common.fsm.transition.Transition;
import net.meta8.common.fsm.transition.EventTransition;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static net.meta8.common.fsm.util.RichOptional.*;

public final class StateConfiguration<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> implements StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> {
  final TState state;

  // state (optional) initialContext. Too bad there's no lazy val in java : can't make it final
  private @NonNull Optional<TLocalContext>                          initialContext = Optional.empty();
  private @NonNull Optional<Function<TLocalContext, TLocalContext>> cloneFunction  = Optional.empty();

  // store outgoing EventTransition(s) by event (if any)
  final @NonNull Map<TEvent, EventTransition<TState, TEvent, TLocalContext, TGlobalContext>>       eventTransitionsWithoutGuards = new HashMap<>();
  final @NonNull Map<TEvent, List<EventTransition<TState, TEvent, TLocalContext, TGlobalContext>>> eventTransitionsWithGuards    = new HashMap<>();

  // store outgoing complement EventTransition (if any). Either (exclusive) complementTransitionWithoutGuard or complementTransitionsWithGuard
  @NonNull Optional<EventTransition<TState, TEvent, TLocalContext, TGlobalContext>> complementTransitionWithoutGuard = Optional.empty();
  @NonNull List<EventTransition<TState, TEvent, TLocalContext, TGlobalContext>>     complementTransitionsWithGuard   = new ArrayList<>();

  // store outgoing TimeoutTransition (if any)
  @NonNull Optional<TimeoutTransition<TState, TEvent, TLocalContext, TGlobalContext>> timeoutTransition = Optional.empty();

  // sequence reflexive transition
  @NonNull Optional<EventTransition<TState, TEvent, TLocalContext, TGlobalContext>> sequenceTransition = Optional.empty();

  // store list of entry actions
  private final @NonNull List<ExecutableAction<TState, TEvent, TLocalContext, TGlobalContext>> onEntryActions = new ArrayList<>();

  // store list of exit optionalAction
  private final @NonNull List<ExecutableAction<TState, TEvent, TLocalContext, TGlobalContext>> onExitActions = new ArrayList<>();

  public StateConfiguration(final @NonNull TState state) {
    this.state = state;
  }

  // -----------------------

  // specify initialContext and it's clone function
  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> initialContext(final @NonNull TLocalContext initialContext,
                                                                                                      final @NonNull Function<TLocalContext, TLocalContext> cloneFunction) {
    this.initialContext = Optional.of(initialContext);
    this.cloneFunction = Optional.of(cloneFunction);
    return (this);
  }

  // -----------------------

  // define an outgoing transition on given triggers
  @SafeVarargs
  @Override
  public final @NonNull StateConfigurationWhenDSL<TState, TEvent, TLocalContext, TGlobalContext> when(final @NonNull TEvent... events) {
    return (new StateConfigurationWhen<>(this, events));
  }

  @SafeVarargs
  @Override
  public final @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptSequence(final @NonNull TEvent... events) {
    ifPresent(sequenceTransition,
              //SOME
              new StateConfigurationError("Sequence already defined. Only one sequence per state is allowed."))
      // NONE
      .orElse(() -> {
        checkSequenceTransitionTriggers(events);
        sequenceTransition = Optional.of(new EventTransition<>(state, Optional.of(state), events));
      });
    return (this);
  }

  @SafeVarargs
  @Override
  public final @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptSequence(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                                                                                                            final @NonNull TEvent...                                             events) {
    ifPresent(sequenceTransition,
              // SOME
              new StateConfigurationError("Sequence already defined. Only one sequence per state is allowed."))
      // NONE
      .orElse(() -> {
        checkSequenceTransitionTriggers(events);
        sequenceTransition = Optional.of(new EventTransition<>(state, Optional.of(state), action, events));
      });
    return (this);
  }

  @SafeVarargs
  @Override
  public final @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptReentrantSequence(final @NonNull TEvent... events) {
    ifPresent(sequenceTransition, (__) -> {throw(new StateConfigurationError("Sequence already defined. Only one sequence per state is allowed."));})
    .orElse( () -> {
      checkSequenceTransitionTriggers(events);
      sequenceTransition = Optional.of(new EventTransition<>(state, Optional.<TState>empty(), events));});
    return (this);
  }

  @SafeVarargs
  @Override
  public final @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptReentrantSequence(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                                                                                                                     final @NonNull TEvent...                                             events) {
    ifPresent(sequenceTransition,
              // SOME
              new StateConfigurationError("Sequence already defined. Only one sequence per state is allowed."))
      // NONE
      .orElse(() -> {
        checkSequenceTransitionTriggers(events);
        sequenceTransition = Optional.of(new EventTransition<>(state, Optional.<TState>empty(), action, events));
      });
    return (this);
  }

  // define an outgoing transition on given triggers complements
  // complements are checked in declaration order after 'when' triggers
  @Override
  public final @NonNull StateConfigurationWhenDSL<TState, TEvent, TLocalContext, TGlobalContext> other() {
    return (new StateConfigurationOther<>(this));
  }

  // -----------------------

  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> moveAfter(final @NonNull Duration duration,
                                                                                                 final @NonNull TState   target) {
    ifPresent(timeoutTransition,
              // SOME
              new StateConfigurationError())
      // NONE
      .orElse(() -> timeoutTransition = Optional.of(new TimeoutTransition<>(state, Optional.of(target), duration)));
    return (this);
  }

  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> moveAfter(final @NonNull Duration                                               duration,
                                                                                                 final @NonNull TState                                                target,
                                                                                                 final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    ifPresent(timeoutTransition,
              // SOME
              new StateConfigurationError())
      // NONE
      .orElse(() -> timeoutTransition = Optional.of(new TimeoutTransition<>(state, Optional.of(target), action, duration)));
    return(this);
  }

  // TODO allow durable timeout transitions
  // define a timeout transition
  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> moveAfterIf(final @NonNull Duration                                             duration,
                                                                                                  final @NonNull TState                                                target,
                                                                                                  final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                                  final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    ifPresent(timeoutTransition,
              // SOME
              new StateConfigurationError())
      // NONE
      .orElse(() -> timeoutTransition = Optional.of(new TimeoutTransition<>(state, Optional.of(target), guard, action, duration)));
    return(this);
  }

  // -----------------------

  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> onEntry(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    onEntryActions.add(new ExecutableAction<>(Optional.empty(), action));
    return(this);
  }

  // define an entry action which will be performed if guard is true
  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> onEntryIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                                 final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    onEntryActions.add(new ExecutableAction<>(Optional.of(guard), action));
    return(this);
  }

  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> onExit(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    onExitActions.add(new ExecutableAction<>(Optional.empty(), action));
    return(this);
  }

  // define an exit action which will be performed if guard is true
  @Override
  public@NonNull  StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> onExitIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                                final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    onExitActions.add(new ExecutableAction<>(Optional.of(guard), action));
    return(this);
  }

  // =====================================================

  public @NonNull Optional<TimeoutTransition<TState, TEvent, TLocalContext, TGlobalContext>> timeoutTransition() {
    return(timeoutTransition);
  }

  // perform entry action(s) on this state configuration when this state is an initial state
  public void performInitialEntryActions(final @NonNull Optional<TLocalContext>  localContext,
                                         final @NonNull Optional<TGlobalContext> machineContext) {
    // create a 'pseudo' transition from 'initial void' to initial state
    final InitialTransition<TState, TEvent, TLocalContext, TGlobalContext> initialTransition = new InitialTransition<>(state);

    // and perform all entry actions on initial state. Note : there's no event in this case
    onEntryActions.forEach(action -> action.perform(null, initialTransition, localContext, localContext, machineContext));
  }

  public Optional<TLocalContext> cloneContext() {
    assert(((! initialContext.isPresent())&&(! cloneFunction.isPresent()))||((initialContext.isPresent())&&(cloneFunction.isPresent())));
    return(initialContext.map(someContext -> cloneFunction.get().apply(someContext)));
  }

  public TState fire(final @NonNull TEvent                                                              events,
                     final @NonNull AtomicInteger                                                       sequenceIndex,
                     final @NonNull Optional<TLocalContext>[]                                           localContexts,
                     final @NonNull Optional<TGlobalContext>                                            globalContext,
                     final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] destinationsConfiguration) throws UnknownTriggerException, MissingStateConfigurationException {
    // event fired by timeout
    if (events == null) {
      final Transition<TState, TEvent, TLocalContext, TGlobalContext> transition = timeoutTransition.get();
      sequenceIndex.set(-1);
      return(performTransition(null,
                               transition,
                               sourceLocalContext(transition, localContexts),
                               destinationLocalContext(transition, localContexts),
                               globalContext,
                               destinationConfiguration(transition, destinationsConfiguration)));
    }
    else {
      final TState sequenceTransitionTarget = processSequenceTransition(events, sequenceIndex, localContexts, globalContext, destinationsConfiguration);
      if (sequenceTransitionTarget != null) {
        return(sequenceTransitionTarget);
      }
      else {
        final TState triggerTransitionTarget = processTriggerTransition(events, localContexts, globalContext, destinationsConfiguration);
        if (triggerTransitionTarget != null) {
          sequenceIndex.set(-1);
          return (triggerTransitionTarget);
        }
        else {
          // try to get first transition satisfying complement events
          final Transition<TState, TEvent, TLocalContext, TGlobalContext> complementTransition = getTriggerComplementsTransition(events, localContexts, globalContext);

          if (complementTransition != null) {
            sequenceIndex.set(-1);
            return (performTransition(events,
                                      complementTransition,
                                      sourceLocalContext(complementTransition, localContexts),
                                      destinationLocalContext(complementTransition, localContexts),
                                      globalContext,
                                      destinationConfiguration(complementTransition, destinationsConfiguration)));
          }
          else {
            throw (new UnknownTriggerException(state.name(), "" + events));
          }
        }
      }
    }
  }

  private TState processSequenceTransition(final @NonNull TEvent                                                              event,
                                           final @NonNull AtomicInteger                                                       sequenceIndex,
                                           final @NonNull Optional<TLocalContext>[]                                           localContexts,
                                           final @NonNull Optional<TGlobalContext>                                            globalContext,
                                           final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] destinationsConfiguration) throws MissingStateConfigurationException, UnknownTriggerException {
    return(getOrElse(sequenceTransition,
                     //SOME
                     (someSequence) -> {
                       final EventTransition<TState, TEvent, TLocalContext, TGlobalContext> someSequenceTransition = sequenceTransition.get();
                       final int index = someSequenceTransition.contains(event);
                       if (index == -1) {
                         return(null);
                       }
                       else {
                         if (index == sequenceIndex.incrementAndGet()) {
                           return (performTransition(event,
                                                     someSequenceTransition,
                                                     sourceLocalContext(someSequenceTransition, localContexts),
                                                     destinationLocalContext(someSequenceTransition, localContexts),
                                                     globalContext,
                                                     destinationConfiguration(someSequenceTransition, destinationsConfiguration)));
                         }
                         else {
                           throw(new UnknownTriggerException(state.name(), event.toString()));
                         }}},
                     //NONE
                     null));
  }

  private TState processTriggerTransition(final @NonNull TEvent                                                              event,
                                          final @NonNull Optional<TLocalContext>[]                                           localContexts,
                                          final @NonNull Optional<TGlobalContext>                                            globalContext,
                                          final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] destinationsConfiguration) throws MissingStateConfigurationException {
    // check if there's at least one event transition without guard associated with this event
    final EventTransition<TState, TEvent, TLocalContext, TGlobalContext> transitionWithoutGuard = eventTransitionsWithoutGuards.get(event);
    if (transitionWithoutGuard != null) {
      return (performTransition(event,
                                transitionWithoutGuard,
                                sourceLocalContext(transitionWithoutGuard, localContexts),
                                destinationLocalContext(transitionWithoutGuard, localContexts),
                                globalContext,
                                destinationConfiguration(transitionWithoutGuard, destinationsConfiguration)));
    }
    // check if there's at least one event transition with guard associated with this event
    else {
      final List<EventTransition<TState, TEvent, TLocalContext, TGlobalContext>> transitionsWithGuards = eventTransitionsWithGuards.get(event);
      if (transitionsWithGuards != null) {
        for (final EventTransition<TState, TEvent, TLocalContext, TGlobalContext> transition : transitionsWithGuards) {
          final Optional<TLocalContext> sourceLocalContext = sourceLocalContext(transition, localContexts);
          final Optional<TLocalContext> destinationLocalContext = destinationLocalContext(transition, localContexts);

          // there's always a guard in this case
          if (transition.guard.get().check(event, transition, sourceLocalContext, destinationLocalContext, globalContext)) {
            return (performTransition(event,
                                      transition,
                                      sourceLocalContext,
                                      destinationLocalContext,
                                      globalContext,
                                      destinationConfiguration(transition, destinationsConfiguration)));
          }
        }
        return (null);
      }
      else {
        return (null); // no event transitions were processed
      }
    }
  }

  private Optional<TLocalContext> sourceLocalContext(final @NonNull Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                                                     final @NonNull Optional<TLocalContext>[]                                localContexts) {
    return(localContexts[transition.source.ordinal()]);
  }

  private Optional<TLocalContext> destinationLocalContext(final @NonNull Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                                                          final @NonNull Optional<TLocalContext>[]                                localContexts) {
    return(localContexts[(transition.destination.isPresent()?transition.destination.get():transition.source).ordinal()]);
  }

  // perform entry action(s) on this state configuration when a transition is triggered
  private void performEntryActions(final @Nullable TEvent                                                    event,
                                   final @NonNull  Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                                   final @NonNull  Optional<TLocalContext>                                   sourceContext,
                                   final @NonNull  Optional<TLocalContext>                                   destinationContext,
                                   final @NonNull  Optional<TGlobalContext>                                  machineContext) {
    onEntryActions.forEach(action -> action.perform(event, transition, sourceContext, destinationContext, machineContext));
  }

  // perform exit action(s) on this state configuration when a transition is triggered
  private void performExitActions(final @Nullable TEvent                                                    event,
                                  final @NonNull  Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                                  final @NonNull  Optional<TLocalContext>                                   sourceContext,
                                  final @NonNull  Optional<TLocalContext>                                   destinationContext,
                                  final @NonNull  Optional<TGlobalContext>                                  machineContext) {
    onExitActions.forEach(action -> action.perform(event, transition, sourceContext, destinationContext, machineContext));
  }

  private TState performTransition(final @Nullable TEvent                                                            event,
                                   final @NonNull  Transition<TState, TEvent, TLocalContext, TGlobalContext>         transition,
                                   final @NonNull  Optional<TLocalContext>                                           sourceContext,
                                   final @NonNull  Optional<TLocalContext>                                           destinationContext,
                                   final @NonNull  Optional<TGlobalContext>                                          machineContext,
                                   final @NonNull  StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> destinationConfiguration) {
    transition.perform(event, transition, sourceContext, destinationContext, machineContext);

    transition.destination.ifPresent(destinationState -> {
      performExitActions(event, transition, sourceContext, destinationContext, machineContext);
      destinationConfiguration.performEntryActions(event, transition, sourceContext, destinationContext, machineContext);
    });

    return(transition.destination.isPresent()?transition.destination.get():transition.source);
  }

  private StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> destinationConfiguration(final @NonNull Transition<TState, TEvent, TLocalContext, TGlobalContext>           transition,
                                                                                                     final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] destinationsConfiguration) throws MissingStateConfigurationException {
    StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> destConfig = destinationsConfiguration[(transition.destination.isPresent()?transition.destination.get():transition.source).ordinal()];
    if (destConfig == null) {

      throw(new MissingStateConfigurationException((transition.destination.isPresent()?transition.destination.get():transition.source).name()));
    }
    else {
      return(destConfig);
    }
  }

  private @Nullable Transition<TState, TEvent, TLocalContext, TGlobalContext> getTriggerComplementsTransition(final @NonNull TEvent                    event,
                                                                                                              final @NonNull Optional<TLocalContext>[] localContexts,
                                                                                                              final @NonNull Optional<TGlobalContext>  machineContext) {
    return(getOrElseSupply(complementTransitionWithoutGuard,
                           // SOME
                           (someTransition) -> someTransition,
                           // NONE
                           () -> {
                             for (final EventTransition<TState, TEvent, TLocalContext, TGlobalContext> complementTriggerTransition : complementTransitionsWithGuard) {
                               if (complementTriggerTransition.guard.get().check(event,
                                                                                 complementTriggerTransition,
                                                                                 sourceLocalContext(complementTriggerTransition, localContexts),
                                                                                 destinationLocalContext(complementTriggerTransition, localContexts),
                                                                                 machineContext)) {
                                 return (complementTriggerTransition);
                               }
                             }
                             return (null);
                           }));
  }

  @SafeVarargs
  private final void checkSequenceTransitionTriggers(final @NonNull TEvent... triggers) {
    for(final TEvent event : triggers) {
      if(eventTransitionsWithoutGuards.containsKey(event)) {
        throw(new StateConfigurationError("Sequence event "+event+" conflicts with existing 'when' triggers"));
      }
      complementTransitionWithoutGuard.ifPresent(someComplementTransition -> {if (someComplementTransition.contains(event)!=-1) throw(new StateConfigurationError("Sequence event "+event+" conflicts with existing 'when' triggers"));});
    }
  }
}
