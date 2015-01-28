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
import net.meta8.common.fsm.action.Guard;
import net.meta8.common.fsm.exception.StateConfigurationError;
import net.meta8.common.fsm.state.States;
import net.meta8.common.fsm.transition.EventTransition;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Optional;

public final class StateConfigurationWhen<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> implements StateConfigurationWhenDSL<TState, TEvent, TLocalContext, TGlobalContext> {
  private final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> configuration;
  private final @NonNull TEvent[] triggers;


  @SafeVarargs
  public StateConfigurationWhen(final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> configuration,
                                final @NonNull TEvent...                                                         triggers) {
    for(final TEvent trigger : triggers) {
      configuration.sequenceTransition.ifPresent(someSequenceTransition -> {if (someSequenceTransition.contains(trigger)!=-1) throw (new StateConfigurationError("Sequence trigger " + trigger + " conflicts with existing 'when' triggers"));});
      configuration.complementTransitionWithoutGuard.ifPresent(someComplementTransition -> {if (someComplementTransition.contains(trigger)!=-1) throw (new StateConfigurationError("Sequence trigger " + trigger + " conflicts with existing 'when' triggers"));});
    }
    this.configuration = configuration;
    this.triggers      = triggers;
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stay() {
    for(final TEvent trigger : triggers) {
      configuration.eventTransitionsWithoutGuards.put(trigger, new EventTransition<>(configuration.state, Optional.<TState>empty(), triggers));
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stay(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    for(final TEvent trigger : triggers) {
      if (configuration.eventTransitionsWithoutGuards.putIfAbsent(trigger, new EventTransition<>(configuration.state, Optional.<TState>empty(), action, triggers)) != null) {
        throw(new StateConfigurationError("Trigger already declared"));
      };
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stayIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard) {
    for(final TEvent trigger : triggers) {
      if (! configuration.eventTransitionsWithGuards.containsKey(trigger)) {
        configuration.eventTransitionsWithGuards.put(trigger, new ArrayList<>());
      }

      configuration.eventTransitionsWithGuards.get(trigger).add(new EventTransition<>(configuration.state, Optional.<TState>empty(), guard, triggers));
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stayIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                           final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    for(final TEvent trigger : triggers) {
      if (! configuration.eventTransitionsWithGuards.containsKey(trigger)) {
        configuration.eventTransitionsWithGuards.put(trigger, new ArrayList<>());
      }

      configuration.eventTransitionsWithGuards.get(trigger).add(new EventTransition<>(configuration.state, Optional.<TState>empty(), guard, action, triggers));
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveTo(final @NonNull TState target) {
    for(final TEvent trigger : triggers) {
      if (configuration.eventTransitionsWithoutGuards.put(trigger, new EventTransition<>(configuration.state, Optional.of(target), triggers)) != null) {
        throw(new StateConfigurationError("Trigger already declared"));
      };
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveTo(final @NonNull TState                                                target,
                                                                                           final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {

    for(final TEvent trigger : triggers) {
      if (configuration.eventTransitionsWithoutGuards.put(trigger, new EventTransition<>(configuration.state, Optional.of(target), action, triggers)) != null) {
        throw(new StateConfigurationError("Trigger already declared"));
      };
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveToIf(final @NonNull TState                                               target,
                                                                                             final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard) {
    for(final TEvent trigger : triggers) {
      if (! configuration.eventTransitionsWithGuards.containsKey(trigger)) {
        configuration.eventTransitionsWithGuards.put(trigger, new ArrayList<>());
      }

      configuration.eventTransitionsWithGuards.get(trigger).add(new EventTransition<>(configuration.state,  Optional.of(target), guard, triggers));
    }

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveToIf(final @NonNull TState                                                target,
                                                                                             final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                             final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    for(final TEvent trigger : triggers) {
      if (! configuration.eventTransitionsWithGuards.containsKey(trigger)) {
        configuration.eventTransitionsWithGuards.put(trigger, new ArrayList<>());
      }

      configuration.eventTransitionsWithGuards.get(trigger).add(new EventTransition<>(configuration.state,  Optional.of(target), guard, action, triggers));
    }

    return(this.configuration);
  }
}
