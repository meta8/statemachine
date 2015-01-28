As I wanted to have a pure java based Finite State Machine DSL I searched on the web existing implementations and found they were too limited for my purpose so I decided to invest some time to create a new one in Java 8.
Java 8 is a nice improvement over Java 7, especially regarding lambda but Optional was quite pityful as it's not a real monad.
Nevertheless Java 8 made the DSL much easier to implement.

# MAIN CONCEPTS

The DSL is based on 3 main concepts

## States

States are expressed as a java Enum which must implement the (empty) marker interface States.

## StateMachineConfiguration

StateMachineConfiguration is used to build a configuration template for a given Finite State Machine specification
It's the conceptual entity that expose the DSL itself and makes use of an existing states enumeration.
Note that each state reachable from a transition must be specified in StateMachineConfiguration.
The DSL is used to specify state behaviour and authorized transitions.

## StateMachine

StateMachine is an executable instance made from a given StateMachineConfiguration. You can have as many StateMachine instances as you want.
They are independent from one each other.


# USAGE

## States

declare a java Enum implementing States interface

eg :
```Java
enum TestStates implements States {
  s1, s2, s3
}
```

there's no restriction other than that on the enum definition

## StateMachineConfiguration

StateMachineConfiguration is a generic class aimed at being as flexible as possible. To this purpose, you'll need
to specify 4 types as illustrated in the following type signature
StateMachineConfiguration<EnumeratedStates, TriggerType, LocalContextType, GlobalContextType>

- EnumeratedStates is the java enum that carries the states

- TriggerType is the type of triggers which will initiate transitions from state to state

- LocalContextType is the type of a context that can be associated with the machine states.
  It allows a state (which is used in given StateMachine instance) to carry internal information if necessary.
  This context is visible from states transitions and their guards (context from the source state and context from the destination state)
  Note that states can not mix different types of context
  Use Void is no context is needed.

- GlobalContextType is the type of a context that can be associated with the machine execution engine instance.
  This context is also visible from states transitions and their guards (context from the source state and context from the destination state)
  Use Void is no context is needed.

To create an empty configuration template, you must instanciate a StateMachineConfiguration through it's constructor.

When no context is needed something as simple as

```Java
final StateMachineConfiguration<TestStates, Character, Void, Void> configuration =
   new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());
```

is enough (TestStates.class refers to the state enumeration)

When a global context is required, you need to specify the type of the context, an initial context instance AND a context clone function using the constructor

```Java
public StateMachineConfiguration(final @NonNull Class<TState> enumStateClazz,
                                 final @NonNull Optional<TGlobalContext> initialContext,
                                 final @NonNull Optional<Function<TGlobalContext, TGlobalContext>> cloneFunction)
```

which gives

```Java
final StateMachineConfiguration<TestStates, Character, Map<Int,String>, Void> configuration =
new StateMachineConfiguration<>(TestStates.class,
                                Optional.of(new HashMap<Int, String>()),
                                Optional.of((originalMap) -> new HashMap<Int,String>(originalMap));
```

The clone function is very important as it's the one that provides isolated copies of configuration context into executable StateMachine instances.
The initial context is not necessarily empty, it could contain pre-existing data.

## DSL syntax

given an existing configuration template instance
final StateMachineConfiguration<TestStates, Character, Void, Void> configuration = ...

The general syntax is

> configuration.state(someState)[.configurationVerb]*;

### configure a state

Create a configuration template for a given state

```Java
configuration.state(final @NonNull TState state). ...
```

eg :
```Java
configuration.state(s1);
```
This create a configuration template for state s1, s1 being an enumeration value
Any state reachable through a transition must have a configuration template even if empty as in

### set state initial context

Assign a state initial context to this state

```Java
configuration.state(s1).initialContext(final @NonNull TLocalContext                          initialContext,
                                       final @NonNull Function<TLocalContext, TLocalContext> cloneFunction). ...
```

eg :
```Java
configuration.state(s1).initialContext(new HashMap<Int, String>(),
                                       (originalMap) -> new HashMap<Int,String>(originalMap))
                       . ...
```

initialContext verb takes 2 parameters :
first one is the state local context initial value (which must satisfy the LocalContextType of StateMachineConfiguration)
second one is the clone function

### set state authorized events

Use when verbs

```Java
configuration.state(s1).when(final @NonNull TEvent... events).[event handler]
```

eg :
```Java
configuration.state(s1).when('a'). ...
                       .when('b'). ...
                       . ...
```
or
```Java
configuration.state(s1).when('a', 'b', 'c').[handler]
                       . ...
```

when several events are specified, it means any of them will trigger the following event handler.
We'll describe event handler later on.

### timeout transition

Move to 'target' state after 'duration' time laps if no other transition occured before.

```Java
configuration.state(s1).moveAfter(final @NonNull Duration duration,
                                  final @NonNull TState   target);
```

eg :
```Java
configuration.state(s1).moveAfter(Duration.ofMillis(1000), s1);
```

Same but also perform 'action' after leaving source state but before reaching target state

```Java
configuration.state(s1).moveAfter(final @NonNull Duration                                              duration,
                                  final @NonNull TState                                                target,
                                  final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);
```

eg :
```Java
configuration.state(s1).moveAfter(Duration.ofMillis(1000), s1,
                                  (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("action"));
```
Same but also perform 'action' after leaving source state but before reaching target state IF 'guard' returns true

```Java
configuration.state(s1).moveAfter(final @NonNull Duration                                              duration,
                                  final @NonNull TState                                                target,
                                  final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                  final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action);
```

eg :
```
configuration.state(s1).moveAfter(Duration.ofMillis(1000), s1,
                                  (character, transition, sourceContext, destinationContext, machineContext) -> true,
                                  (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("action"));
```

### sequenced events

Loop on the same state for the specified ORDERED event sequence : each event is expected in the given order.
An UnknownTriggerException is thrown if an unexpected event is met before the end of the expected sequence
onEntry actions are performed only on the first event of the sequence

```Java
configuration.state(s1).acceptSequence(final @NonNull TEvent... events)
```

eg :
```Java
configuration.state(s1).acceptSequence('a','b','c'). ...
```

Same but 'action' is performed for each event of the sequence

```Java
configuration.state(s1).acceptSequence(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action,
                                       final @NonNull TEvent...                                             events)
```

eg :
```Java
configuration.state(s1).acceptSequence((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("action"),
                                       'a','b','c'). ...
```

acceptReentrantSequence(...) methods are identical but onEntry actions are performed only all events of the sequence

### specify default events

it's sometime useful to express an event handler which is triggered when one receives an event not handled by 'when' handler.
'other' handler fits this purpose

```Java
configuration.state(s1).other().[event handler]
```

We'll describe event handler later on.
It's recommended but not mandatory to specify other() at the end of the state specification as in
```Java
configuration.state(s1)
             .when('a'). ...
             .when('b'). ...
             .other(). ...
```

### on entry, on exit actions

Specify actions to perform when entering a state
```Java
configuration.state(s1).onEntry(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action). ...
```

eg :
```Java
configuration.state(s1)
                 .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry"))
                 . ...
```

Same but action is performed only if guard is satisfied
```Java
configuration.state(s1).onEntryIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                  final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action). ...
```

eg :
```Java
configuration.state(s1)
             .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                        (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry s1"))
             . ...
```

Specify actions to perform when exiting a state
```Java
configuration.state(s1).onExit(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action). ...
```

eg :
```Java
configuration.state(s1)
             .onExit((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit s1"))
             . ...
```

Same but action is performed only if guard is satisfied
```Java
configuration.state(s1).onEntryIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
                                  final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action). ...
```

eg :
```Java
configuration.state(s1)
             .onExitIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                       (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit s1"))
             . ...
```

It's recommended but not mandatory to specify onEntry() and onExit() at the beginning of the state specification as in
```Java
configuration.state(s1)
             .onEnty(...)
             .onExit(...)
             .when('a'). ...
             .when('b'). ...
```

## EVENT HANDLERS

Move to another state
```Java
moveTo(final @NonNull TState target)
```

Move to another state and perform an action after leaving source state but before entering target state
```Java
moveTo(final @NonNull TState                                                target,
       final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action)
```

Move to another state if guard is verified
```Java
moveToIf(final @NonNull TState                                               target,
         final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard)
```

Move to another state if guard is verified and perform an action after leaving source state but before entering target state
```Java
moveToIf(final @NonNull TState                                                target,
         final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
         final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action)
```

Note that target state can be the same as the source state. In this case one defines a reflexive transition

eg :
```Java
configuration.state(s1)
             .onEntry(...)
             .onExit(...)
             .moveTo(s1)
```

In this case existing entry actions are performed each time the transition is triggered

When you have a reflexive transition and don't want to perform entry actions each time you re-enter the state, you must use one of the 'stay' action verb

Stay in the same state and do not perform existing entry actions
```Java
stay()
```

Stay in the same state and do not perform existing entry actions and perform the specified action
```Java
stay(final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action)
```

Stay in the same state and do not perform existing entry actions if the guard is verified
```Java
stayIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext> guard)
```

Stay in the same state and do not (re-)perform existing entry actions if the guard is verified and perform the specified action
```Java
stayIf(final @NonNull Guard<TState, TEvent, TLocalContext, TGlobalContext>  guard,
       final @NonNull Action<TState, TEvent, TLocalContext, TGlobalContext> action)
```

## GUARDS

A guard is a boolean condition which must be verified if transition is to be triggered. It's implemented as a FunctionInterface so that lambda can be used

```Java
@FunctionalInterface
public interface Guard<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {
  boolean check(final @Nullable TEvent                                                    event,
                final @NonNull  Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
                final @NonNull  Optional<TLocalContext>                                   sourceContext,
                final @NonNull  Optional<TLocalContext>                                   destinationContext,
                final @NonNull  Optional<TGlobalContext>                                  machineContext);
}
```

**Note** :  event is null is the guard is triggered by a timeout transition

and a corresponding lambda example :

```Java
(event, transition, sourceContext, destinationContext, machineContext) -> true
```

Note that a guard has access to the state local context of the source and of the destination and also to the global state machine context.

## ACTIONS

An action which can be triggered on a transition or onEntry/onExit callbacks. It's implemented as a FunctionInterface so that lambda can be used

```Java
@FunctionalInterface
public interface Action<TState extends Enum<TState> & States, TEvent, TLocalContext, TGlobalContext> {
  void perform(final @Nullable TEvent                                                    event,
               final @NonNull  Transition<TState, TEvent, TLocalContext, TGlobalContext> transition,
               final @NonNull  Optional<TLocalContext>                                   sourceContext,
               final @NonNull  Optional<TLocalContext>                                   destinationContext,
               final @NonNull  Optional<TGlobalContext>                                  machineContext);
}
```

**Note** :  event is null is the guard is triggered by a timeout transition

and a corresponding lambda example :

```Java
(event, transition, sourceContext, destinationContext, machineContext) -> System.out.println(""+event+" has triggered transition "+transition);
```

Note that an action has access to the state local context of the source and of the destination and also to the global state machine context.


## Example

```Java
class SimpleFSM {
  @Test
  public void simpleExample() throws UnknownTriggerException, MissingStateConfigurationException {

    // CREATE AN EMPTY FSM CONFIGURATION TEMPLATE
    final StateMachineConfigurationDSL<TestStates, Character, Void, Void> configuration = new StateMachineConfiguration<>(TestStates.class, Optional.empty(), Optional.empty());

    // CONFIGURE TRANSITIONS ON STATE s1
    configuration.state(s1)
                 .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                            (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry s1"))
                 .onExitIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                           (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit s1"))
                 .when('a').moveToIf(s2,
                                     (character, transition, sourceContext, destinationContext, machineContext) -> true,
                                     (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("moveTo to s2"))
                 .when('b').moveTo(s2);

    // CONFIGURE TRANSITIONS ON STATE s2
    configuration.state(s2)
                 .onEntryIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                            (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry s2"))
                 .onExitIf((character, transition, sourceContext, destinationContext, machineContext) -> true,
                           (character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit S2"))
                 .when('c').moveTo(s3);

    // CONFIGURE TRANSITIONS ON STATE s3
    configuration.state(s3)
                 .onEntry((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onEntry S3"))
                 .onExit((character, transition, sourceContext, destinationContext, machineContext) -> System.out.println("onExit S3"))
                 .when('c').moveTo(s3)
                 .when('d').stay();

    // CREATE AN EXECUTABLE FSM INSTANCE FROM THE CONFIGURATION TEMPLATE
    final StateMachine<TestStates, Character, Void, Void> machine = new StateMachine<>(TestStates.class, configuration);

    TestStates targetState;

    // AND FIRE SOME EVENTS
    targetState = machine.fire('a');
    System.out.println("Current state is : "+targetState);
    targetState = machine.fire('c');
    System.out.println("Current state is : "+targetState);
    targetState = machine.fire('d');
    System.out.println("Current state is : "+targetState);
    targetState = machine.fire('c');
    System.out.println("Current state is : "+targetState);
  }
}

enum TestStates implements States {
  s1, s2, s3
}
```