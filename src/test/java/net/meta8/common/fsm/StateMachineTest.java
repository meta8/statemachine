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

package net.meta8.common.fsm;

import net.meta8.common.fsm.configuration.StateMachineConfiguration;
import net.meta8.common.fsm.configuration.StateMachineConfigurationDSL;
import net.meta8.common.fsm.exception.MissingStateConfigurationException;
import net.meta8.common.fsm.exception.UnknownTriggerException;
import net.meta8.common.fsm.machine.StateMachine;
import net.meta8.common.fsm.state.States;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static net.meta8.common.fsm.TestStates.*;
import static org.junit.Assert.*;

public class StateMachineTest {
  @Test
  public void invalidInitializationTest() {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());
    try {
      new StateMachine<>(TestStates.class, configuration);
      fail("MissingStateConfiguration expected");
    }
    catch(MissingStateConfigurationException missingStateConfiguration) {
      //expected
    }
  }

  @Test
  public void invalidTriggerTest() throws MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    configuration.state(s1)
                 .when('a').stay();

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    try {
      final TestStates s = machine.fire('b');
      fail("Should not work");
    }
    catch(final UnknownTriggerException e) {
      // expected
    }
  }

  @Test
  public void invalidTargetTest() throws MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    configuration.state(s1)
                 .when('a').moveTo(s2);

    // s2 is undeclared !

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    try {
      final TestStates s = machine.fire('a');
      fail("Should not work");
    }
    catch(final MissingStateConfigurationException e) {
      // expected
    }
    catch (UnknownTriggerException unknownTrigger) {
      fail("Should not throw a UnknownTriggerException");
    }
  }

  @Test
  public void initialOnEntryTest() throws UnknownTriggerException, MissingStateConfigurationException {
    // no guard
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean entry = new AtomicBoolean(false);

      configuration.state(s1)
                   .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> entry.compareAndSet(false, true));

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      assertTrue(entry.get());
    }

    // guard OK
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean entry = new AtomicBoolean(false);

      configuration.state(s1)
                   .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                              (character, transition, sourceContext, destinationContext, machineContext) -> entry.compareAndSet(false, true));

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      assertTrue(entry.get());
    }

    // guard NOK
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean entry = new AtomicBoolean(false);

      configuration.state(s1)
                   .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> false,
                              (character, transition, sourceContext, destinationContext, machineContext) -> entry.compareAndSet(false, true));

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      assertFalse(entry.get());
    }
  }

  @Test
  public void simpleTransitionTest() throws UnknownTriggerException, MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    configuration.state(s1)
                 .when('a').moveTo(s2);

    configuration.state(s2);

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    final TestStates s = machine.fire('a');
    assertEquals(s2, s);
  }

  @Test
  public void simpleTransitionWithExitTest() throws UnknownTriggerException, MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    final AtomicBoolean exit = new AtomicBoolean(false);

    configuration.state(s1)
                 .onExit((character, transition, sourceContext, destinationContext, machineContext) -> exit.compareAndSet(false, true))
                 .when('a').moveTo(s2);

    configuration.state(s2);

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    final TestStates s = machine.fire('a');
    assertEquals(s2, s);
    assertTrue(exit.get());
  }

  @Test
  public void simpleTransitionWithEntryTest() throws UnknownTriggerException, MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    final AtomicBoolean entry = new AtomicBoolean(false);

    configuration.state(s1)
                 .when('a').moveTo(s2);

    configuration.state(s2)
                 .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> entry.compareAndSet(false, true));

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    final TestStates s = machine.fire('a');
    assertEquals(s2, s);
    assertTrue(entry.get());
  }

  @Test
  public void simpleTransitionWithExitEntryTest() throws UnknownTriggerException, MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    final AtomicBoolean entry = new AtomicBoolean(false);
    final AtomicBoolean exit  = new AtomicBoolean(false);

    configuration.state(s1)
                 .onExit((character, transition, sourceContext, destinationContext, machineContext) -> exit.compareAndSet(false, true))
                 .when('a').moveTo(s2);

    configuration.state(s2)
                 .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> entry.compareAndSet(false, true));

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    final TestStates s = machine.fire('a');
    assertEquals(s2, s);
    assertTrue(exit.get());
    assertTrue(entry.get());
  }

  @Test
  public void reflexiveTest() throws UnknownTriggerException, MissingStateConfigurationException {
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean initial = new AtomicBoolean(true);
      final AtomicBoolean entry   = new AtomicBoolean(false);

      configuration.state(s1)
                   .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> {
                     if (initial.get()) {
                       initial.set(false);
                     }
                     else {
                       entry.compareAndSet(false, true);
                     }
                   })
                   .when('a').stay();

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s = machine.fire('a');
      assertEquals(s1, s);
      assertFalse(entry.get());
    }
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean initial = new AtomicBoolean(true);
      final AtomicBoolean entry   = new AtomicBoolean(false);
      final AtomicBoolean action  = new AtomicBoolean(false);

      configuration.state(s1)
                   .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> {
                     if (initial.get()) {
                       initial.set(false);
                     }
                     else {
                       entry.compareAndSet(false, true);
                     }
                   })
                   .when('a').stay((character, transition, sourceContext, destinationContext, machineContext) -> action.compareAndSet(false, true));

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s = machine.fire('a');
      assertEquals(s1, s);
      assertFalse(entry.get());
      assertTrue(action.get());
    }
  }

  @Test
  public void reflexiveWithEntryAndGuardTest() throws UnknownTriggerException, MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    final AtomicBoolean entry = new AtomicBoolean(false);
    final AtomicInteger entryGuard = new AtomicInteger(0);

    configuration.state(s1)
                 .when('a').moveTo(s2);
    configuration.state(s2)
                 .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> (entryGuard.incrementAndGet() % 2 == 0),
                            (character, transition, sourceContext, destinationContext, machineContext) -> entry.compareAndSet(false, true))
                 .when('b').moveTo(s2);

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    TestStates s = machine.fire('a');
    assertEquals(s2, s);
    assertFalse(entry.get());

    s = machine.fire('b');
    assertEquals(s2, s);
    assertTrue(entry.get());
  }

  @Test
  public void sequenceTest() throws UnknownTriggerException, MissingStateConfigurationException {
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean entry = new AtomicBoolean(false);

      configuration.state(s1)
                   .when('0').moveTo(s2);

      configuration.state(s2)
                   .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> {
                     if (!entry.compareAndSet(false, true)) entry.compareAndSet(true, false);
                   })
                   .acceptSequence('a', 'b', 'c')
                   .when('d').moveTo(s3);

      configuration.state(s3);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s_0 = machine.fire('0');
      System.out.println("0 -> "+s_0);
      assertEquals(s2, s_0);
      assertTrue(entry.get());

      final TestStates s_a = machine.fire('a');
      System.out.println("a -> "+s_a);
      assertEquals(s2, s_a);
      assertFalse(entry.get());

      final TestStates s_b = machine.fire('b');
      System.out.println("b -> "+s_b);
      assertEquals(s2, s_b);
      assertTrue(entry.get());

      final TestStates s_c = machine.fire('c');
      System.out.println("c -> "+s_c);
      assertEquals(s2, s_c);
      assertFalse(entry.get());

      final TestStates s_d = machine.fire('d');
      System.out.println("d -> "+s_d);
      assertEquals(s3, s_d);
    }
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      final AtomicBoolean entry = new AtomicBoolean(false);

      configuration.state(s1)
                   .when('0').moveTo(s2);

      configuration.state(s2)
                   .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> {
                     if (!entry.compareAndSet(false, true)) entry.compareAndSet(true, false);
                   })
                   .acceptReentrantSequence('a', 'b', 'c')
                   .when('d').moveTo(s3);

      configuration.state(s3);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s_0 = machine.fire('0');
      System.out.println("0 -> "+s_0);
      assertEquals(s2, s_0);
      assertTrue(entry.get());

      final TestStates s_a = machine.fire('a');
      System.out.println("a -> "+s_a);
      assertEquals(s2, s_a);
      assertTrue(entry.get());

      final TestStates s_b = machine.fire('b');
      System.out.println("b -> "+s_b);
      assertEquals(s2, s_b);
      assertTrue(entry.get());

      final TestStates s_c = machine.fire('c');
      System.out.println("c -> "+s_c);
      assertEquals(s2, s_c);
      assertTrue(entry.get());

      final TestStates s_d = machine.fire('d');
      System.out.println("d -> "+s_d);
      assertEquals(s3, s_d);
    }
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      configuration.state(s1)
                   .acceptSequence('a', 'b', 'c');

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s_a = machine.fire('a');
      assertEquals(s1, s_a);

      try {
        final TestStates s_b = machine.fire('d');
        fail("Should not accept out of sequence trigger");
      }
      catch(UnknownTriggerException e) {
        // expected
      }
    }
  }

  @Test
  public void complementTest() throws MissingStateConfigurationException, UnknownTriggerException {
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      configuration.state(s1)
                   .when('a', 'b').moveTo(s2)
                   .other().stay();

      configuration.state(s2);

      configuration.state(s3);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s = machine.fire('c');
      assertEquals(s1, s);
    }
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      configuration.state(s1)
                   .when('a', 'b').moveTo(s2)
                   .other().moveTo(s3);

      configuration.state(s2);

      configuration.state(s3);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s = machine.fire('c');
      assertEquals(s3, s);
    }
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      configuration.state(s1)
                   .when('a', 'b').moveTo(s2)
                   .other().moveToIf(s3, (character, transition, sourceContext, destinationContext, machineContext) -> character.equals('c'))
                   .other().moveToIf(s2, (character, transition, sourceContext, destinationContext, machineContext) -> character.equals('d'));

      configuration.state(s2);

      configuration.state(s3);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      final TestStates s = machine.fire('c');
      assertEquals(s3, s);
    }
  }

  @Test
  public void timeoutTransitionTest() throws UnknownTriggerException, MissingStateConfigurationException, InterruptedException {
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());
      final AtomicLong count = new AtomicLong();

      configuration.state(s1)
                   .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> count.incrementAndGet())
                   .moveAfter(Duration.ofMillis(1000), s1);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      assertEquals(1, count.intValue());

      Thread.sleep(3500);

      assertEquals(s1, machine.getCurrentState());
      assertEquals(4, count.intValue());

      machine.close();
    }
    System.out.println("------------------");
    {
      final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

      configuration.state(s1)
                   .moveAfter(Duration.ofMillis(3000), s2);

      configuration.state(s2);

      final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

      Thread.sleep(4000);

      assertEquals(s2, machine.getCurrentState());
    }
  }

  @Ignore
  @Test
  public void timeoutTransitionWithoutExitEntryTest() throws MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());
    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);
  }

  @Ignore
  @Test
  public void timeoutTransitionWithExitEntryTest() throws MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());
    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);
  }

  @Ignore
  @Test
  public void multipleStateMachineTest() throws MissingStateConfigurationException {
    final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());
    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);
  }

  @Test
  public void simpleExample() throws UnknownTriggerException, MissingStateConfigurationException {
    final StateMachineConfigurationDSL<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    configuration.state(s1)
                 .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                            (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry s1"))
                 .onExitIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                           (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit s1"))
                 .when('a').moveToIf(s2,
                                     (character, transition, sourceContext, destinationContext, machineContext) -> true,
                                     (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("moveTo to s2"))
                 .when('b').moveTo(s2);

    configuration.state(s2)
                 .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                            (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry s2"))
                 .onExitIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                           (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit S2"))
                 .when('c').moveTo(s3);

    configuration.state(s3)
                 .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry S3"))
                 .onExit((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit S3"))
                 .when('c').moveTo(s3)
                 .when('d').stay();

    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    TestStates s = TestStates.s1;
    System.out.println("From "+s.name()+" - fire 'a'");
    s = machine.fire('a');
    System.out.println("Now : "+s.name());

    System.out.println("From "+s.name()+" - fire 'c'");
    s = machine.fire('c');
    System.out.println("Now : "+s.name());

    System.out.println("From "+s.name()+" - fire 'd'");
    s = machine.fire('d');
    System.out.println("Now : "+s.name());

    System.out.println("From "+s.name()+" - fire 'c'");
    s = machine.fire('c');
    System.out.println("Now : "+s.name());
  }
}

enum TestStates implements States {
  s1, s2, s3
}
