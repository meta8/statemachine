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

package net.meta8.common.fsm.action;

import net.meta8.common.fsm.state.States;
import net.meta8.common.fsm.transition.Transition;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import static net.meta8.common.fsm.util.RichOptional.ifPresent;

public final class ExecutableAction<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {
  public final Optional<Guard<TState, TEvent, TLocalContext, TGlobalContext>> guard;
  public final Action<TState, TEvent, TLocalContext, TGlobalContext> action;

  public ExecutableAction(final @NonNull Optional<Guard<TState, TEvent, TLocalContext, TGlobalContext>> guard,
                          final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext>          action) {
    this.guard  = guard;
    this.action = action;
  }

  public void perform(final @Nullable TEvent                                                   event,   // null if triggered by a timeout transition
                      final @NonNull Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                      final @NonNull Optional<TLocalContext>                                   sourceContext,
                      final @NonNull Optional<TLocalContext>                                   destinationContext,
                      final @NonNull Optional<TGlobalContext>                                  machineContext) {
    ifPresent(guard, (someGuard) -> {if (someGuard.check(event, transition, sourceContext, destinationContext, machineContext)) {
      action.perform(event, transition, sourceContext, destinationContext, machineContext);
    }})
      .orElse(() -> action.perform(event, transition, sourceContext, destinationContext, machineContext) );
  }
}
