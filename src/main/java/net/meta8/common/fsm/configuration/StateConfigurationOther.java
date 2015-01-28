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

import java.util.Optional;

public final class StateConfigurationOther<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> implements StateConfigurationWhenDSL<TState, TEvent, TLocalContext, TGlobalContext> {
  private final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> configuration;

  public StateConfigurationOther(final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> configuration) {
    this.configuration = configuration;
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stay() {
    if ((configuration.complementTransitionWithoutGuard.isPresent())||(! configuration.complementTransitionsWithGuard.isEmpty())) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionWithoutGuard = Optional.of(new EventTransition<>(configuration.state, Optional.<TState>empty()));

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stay(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    if ((configuration.complementTransitionWithoutGuard.isPresent())||(! configuration.complementTransitionsWithGuard.isEmpty())) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionWithoutGuard = Optional.of(new EventTransition<>(configuration.state, Optional.<TState>empty(), action));

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stayIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard) {
    if (configuration.complementTransitionWithoutGuard.isPresent()) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionsWithGuard.add(new EventTransition<>(configuration.state, Optional.<TState>empty(), guard));

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stayIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                           final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    if (configuration.complementTransitionWithoutGuard.isPresent()) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionsWithGuard.add(new EventTransition<>(configuration.state, Optional.<TState>empty(), guard, action));

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveTo(final @NonNull TState target) {

    if ((configuration.complementTransitionWithoutGuard.isPresent())||(! configuration.complementTransitionsWithGuard.isEmpty())) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionWithoutGuard = Optional.of(new EventTransition<>(configuration.state, Optional.of(target)));


    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveTo(final @NonNull TState                                                target,
                                                                                           final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    if ((configuration.complementTransitionWithoutGuard.isPresent())||(! configuration.complementTransitionsWithGuard.isEmpty())) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionWithoutGuard = Optional.of(new EventTransition<>(configuration.state, Optional.of(target), action));

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveToIf(final @NonNull TState                                               target,
                                                                                             final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard) {
    if (configuration.complementTransitionWithoutGuard.isPresent()) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionsWithGuard.add(new EventTransition<>(configuration.state, Optional.of(target), guard));

    return(this.configuration);
  }

  @Override
  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> moveToIf(final @NonNull TState                                                target,
                                                                                             final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                             final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    if (configuration.complementTransitionWithoutGuard.isPresent()) {
      throw(new StateConfigurationError());
    }
    configuration.complementTransitionsWithGuard.add(new EventTransition<>(configuration.state, Optional.of(target), guard, action));

    return(this.configuration);
  }
}
