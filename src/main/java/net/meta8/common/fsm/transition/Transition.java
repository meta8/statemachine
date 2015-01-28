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

package net.meta8.common.fsm.transition;

import net.meta8.common.fsm.action.Action;
import net.meta8.common.fsm.action.Guard;
import net.meta8.common.fsm.state.States;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public abstract class Transition<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {

  public final @NonNull TState                                                          source;      // transition source state
  public final @NonNull Optional<TState>                                                destination; // transition destination state. If empty : re-entrant transition without triggering onEntry. If source == destination : onEntry is triggered
  public final @NonNull Optional<Guard<TState, TEvent, TLocalContext, TGlobalContext>>  guard;
  public final @NonNull Optional<Action<TState, TEvent, TLocalContext, TGlobalContext>> action;      // action bound to transition

  public Transition(final @NonNull TState           source,
                    final @NonNull Optional<TState> destination) {
    this.source      = source;
    this.destination = destination;
    this.guard       = Optional.empty();
    this.action      = Optional.empty();
  }

  public Transition(final @NonNull TState                                               source,
                    final @NonNull Optional<TState>                                     destination,
                    final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard) {
    this.source      = source;
    this.destination = destination;
    this.guard       = Optional.of(guard);
    this.action      = Optional.empty();
  }

  public Transition(final @NonNull TState                                                source,
                    final @NonNull Optional<TState>                                      destination,
                    final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    this.source      = source;
    this.destination = destination;
    this.guard       = Optional.empty();
    this.action      = Optional.of(action);
  }

  public Transition(final @NonNull TState                                                source,
                    final @NonNull Optional<TState>                                      destination,
                    final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                    final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action) {
    this.source      = source;
    this.destination = destination;
    this.guard       = Optional.of(guard);
    this.action      = Optional.of(action);
  }

  public void perform(final @Nullable TEvent                                                   trigger,   // null if triggered by a timeout transition
                      final @NonNull Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                      final @NonNull Optional<TLocalContext>                                   sourceContext,
                      final @NonNull Optional<TLocalContext>                                   destinationContext,
                      final @NonNull Optional<TGlobalContext>                                  machineContext) {
    action.ifPresent(someAction -> {
      someAction.perform(trigger, transition, sourceContext, destinationContext, machineContext);
    });
  }
}
