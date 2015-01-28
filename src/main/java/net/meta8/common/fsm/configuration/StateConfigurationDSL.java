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
import net.meta8.common.fsm.state.States;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.util.function.Function;

public interface StateConfigurationDSL<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {
  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> initialContext(final @NonNull TLocalContext                          initialContext,
                                                                                                      final @NonNull Function<TLocalContext, TLocalContext> cloneFunction);

  public @NonNull StateConfigurationWhenDSL<TState, TEvent, TLocalContext, TGlobalContext> when(final @NonNull TEvent... events);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptSequence(final @NonNull TEvent... triggers);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptSequence(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                                                                                                     final @NonNull TEvent...                                             events);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptReentrantSequence(final @NonNull TEvent... events);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> acceptReentrantSequence(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                                                                                                               final @NonNull TEvent...                                             events);

  public @NonNull StateConfigurationWhenDSL<TState, TEvent, TLocalContext, TGlobalContext> other();

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     moveAfter(final @NonNull Duration duration,
                                                                                                     final @NonNull TState   target);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     moveAfter(final @NonNull Duration                                              duration,
                                                                                                     final @NonNull TState                                                target,
                                                                                                     final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     moveAfterIf(final @NonNull Duration                                              duration,
                                                                                                       final @NonNull TState                                                target,
                                                                                                       final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                                       final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     onEntry(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     onEntryIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                                     final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     onExit(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);

  public @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext>     onExitIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                                                                                    final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);
}
