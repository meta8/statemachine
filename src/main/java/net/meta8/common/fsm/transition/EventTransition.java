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

public final class EventTransition<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> extends Transition<TState, TEvent, TLocalContext, TGlobalContext> {
  public final @Nullable TEvent[] triggers;

  public EventTransition(final @NonNull TState source,
                         final @NonNull Optional<TState> destination) {
    super(source, destination);
    this.triggers = null;
  }

  @SafeVarargs
  public EventTransition(final @NonNull TState           source,
                         final @NonNull Optional<TState> destination,
                         final @NonNull TEvent...        triggers) {
    super(source, destination);
    this.triggers = triggers;
  }

  @SafeVarargs
  public EventTransition(final @NonNull TState                                               source,
                         final @NonNull Optional<TState>                                     destination,
                         final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard,
                         final @NonNull TEvent...                                            triggers) {
    super(source, destination, guard);
    this.triggers = triggers;
  }

  @SafeVarargs
  public EventTransition(final @NonNull TState                                                source,
                         final @NonNull Optional<TState>                                      destination,
                         final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                         final @NonNull TEvent...                                             triggers) {
    super(source, destination, action);
    this.triggers = triggers;
  }

  @SafeVarargs
  public EventTransition(final @NonNull TState                                                source,
                         final @NonNull Optional<TState>                                      destination,
                         final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                         final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                         final @NonNull TEvent...                                             triggers) {
    super(source, destination, guard, action);
    this.triggers = triggers;
  }

  public int contains(final @NonNull TEvent trigger) {
    if (triggers == null) {
      return(-1);
    }
    else {
      for(int i=0; i<triggers.length; ++i) {
        if (trigger.equals(triggers[i])) return(i);
      }
      return(-1);
    }
  }
}
