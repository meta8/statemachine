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

import net.meta8.common.fsm.exception.MissingStateConfigurationException;
import net.meta8.common.fsm.state.States;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public final class StateMachineConfiguration<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> implements StateMachineConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> {
  // machine initial state
  private TState initialState;

  // machine initialContext
  private final @NonNull Optional<TGlobalContext> initialContext;
  private final @NonNull Optional<Function<TGlobalContext, TGlobalContext>> cloneFunction;

  // states configurations : an array of StateConfiguration for each state
  private final @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] stateMachineConfiguration;


  @SuppressWarnings("unchecked")
  public StateMachineConfiguration(final @NonNull Class<TState>                                      enumStateClazz,
                                   final @NonNull Optional<TGlobalContext>                           initialContext,
                                   final @NonNull Optional<Function<TGlobalContext, TGlobalContext>> cloneFunction) {
    stateMachineConfiguration = (StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[]) Array.newInstance(StateConfiguration.class, enumStateClazz.getEnumConstants().length);
    this.initialContext       = initialContext;
    this.cloneFunction        = cloneFunction;
  }

  // -----------------------

  public Optional<TGlobalContext> cloneContext() {
    assert(((! initialContext.isPresent())&&(! cloneFunction.isPresent()))||((initialContext.isPresent())&&(cloneFunction.isPresent())));
    return(initialContext.map(someContext -> cloneFunction.get().apply(someContext)));
  }

  @Override
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> state(final @NonNull TState state) {
    if (initialState == null) {
      initialState = state;
    }

    final StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext> stateConfiguration = new StateConfiguration<>(state);
    stateMachineConfiguration[state.ordinal()] = stateConfiguration;
    return(stateConfiguration);
  }

  // -----------------------

  public @NonNull TState getInitialState() throws MissingStateConfigurationException {
    if (initialState == null) throw(new MissingStateConfigurationException("initial state"));
    return(initialState);
  }

  public @NonNull StateConfiguration<TState, TEvent, TLocalContext, TGlobalContext>[] getStateMachineConfigurationClone() {
    return(Arrays.copyOf(stateMachineConfiguration, stateMachineConfiguration.length));
  }
}
