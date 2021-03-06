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

import net.meta8.common.fsm.state.States;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface StateMachineConfigurationDSL<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {
  @NonNull StateConfigurationDSL<TState, TEvent, TLocalContext, TGlobalContext> state(final @NonNull TState state);
}
