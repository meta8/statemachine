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

package net.meta8.common.fsm.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/*
import static net.meta8.common.fsm.util.RichOptional.*;

final Optional opt = ...
ifPresent(opt, (x) -> ...).orElse( () -> ... )
 */
public abstract class RichOptional {
  public abstract void orElse(final Runnable runner);
  public abstract void orElseThrow(final Throwable throwable) throws Throwable;

  public static <T> RichOptional ifPresent(final Optional<T> opt, final Consumer<? super T> consumer) {
    if (opt.isPresent()) {
      consumer.accept(opt.get());
      return(new OptionalDoNothing());
    }
    return(new OptionalDoSomething());
  }

  public static <T> RichOptional ifPresent(final Optional<T> opt, final Throwable throwable) throws Throwable {
    if (opt.isPresent()) {
      throw(throwable);
    }
    return(new OptionalDoSomething());
  }

  public static <T> RichOptional ifPresent(final Optional<T> opt, final Error error) {
    if (opt.isPresent()) {
      throw(error);
    }
    return(new OptionalDoSomething());
  }

  public static <T, R> R getOrElse(final Optional<T> opt, final Function<? super T, ? extends R> onSome, final R value) {
    if (opt.isPresent()) {
      return(onSome.apply(opt.get()));
    }
    return(value);
  }

  public static <T, R> R getOrElseSupply(final Optional<T> opt, final Function<? super T, ? extends R> onSome, final Supplier<? extends R> onNone) {
    if (opt.isPresent()) {
      return(onSome.apply(opt.get()));
    }
    return(onNone.get());
  }


}

final class OptionalDoSomething extends RichOptional {
  public void orElse(final Runnable runner) {
      runner.run();
  }

  public void orElseThrow(final Throwable throwable) throws Throwable {
    throw(throwable);
  }
}

final class OptionalDoNothing extends RichOptional {
  public void orElse(final Runnable runner) {}

  public void orElseThrow(final Throwable throwable) throws Throwable {}
}