![Dirigent](https://github.com/CubeEngine/Dirigent/blob/master/Dirigent.png?raw=true)
=================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.cubeengine/dirigent/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.cubeengine/dirigent)
[![Build Status](https://travis-ci.org/CubeEngine/Dirigent.svg?branch=master)](https://travis-ci.org/CubeEngine/Dirigent)

The Dirigent project is a compact formatting framework. It can be used to place message parameters into a message elegantly. This can be done by specifying so called macros within a message which will be replaced by the correct input parameters in the desired way. Therefore it can be used in conjunction with translated messages perfectly.

# Macro

A macro is a special sequence of letters which is replaced with an input parameter from Dirigent using a Formatter. It must be placed within a string message. Furthermore it's possible to specify more than one macro in a single message. In this context macros follow the syntax:

 ```{[[<position>:]type[#<label>][:<args>]]}``` or just ```{[position]}```
 
Ensuing from the syntax here are the possibilities:

1. Using default macros: "Hello {} {}, how are you doing?"; A default macro only uses a default formatter which can be specified with the Dirigent constructor. It doesn't provide any contextual information. The input parameters are inserted into the message in the same order as they're specified.
2. Using indexed macros: "Hello {0} {1}, how are you doing?"; When translating a message with multiple arguments the translator might have to change the order in which the arguments are. Different languages have a different order of the parameter occurrences often. Therefore it must be possible to specify the position of the parameter in the input parameter list. Keep in mind positions do start with 0 not with 1! Simple indexed parameters use the default formatter as well.
3. Using a different formatter: "Hello {name}, how are you doing?"; In this example the macro value will be generated by a formatter handling the name `name`. The formatter can do whatever it wants. To provide locale based parameter values it gets a compose context. This also contains additional information. But a formatter just formats a single input parameter.
4. Using a different formatter and an index: "Hello {0:name}, how are you doing?"; It's also possible to add an optional position to the formatter. This has the same effect as for the default macro.
5. Using a formatter with special arguments: "Date: {date:format=yyyy-MM-dd}"; A formatter can get different arguments to do the job. Every argument is split with a `:`. There are two types of different arguments. A parameter argument maps a argument name to a value like the example illustrates it. A value argument only has a value. This could be a flag.
6. Providing additional information: "Hello {0:name#name of the user:some-argument}, how are you doing?"; A label is a kind of contextual information for the localizer. This won't be available within the code and only helps the localizer to understand the meaning of the parameter. A label can be used without specifying an argument too.

The label as well as the arguments are allowed to contain any character except `:`, `}` and `\` which have to be escaped using `\`.

# Process

The Dirigent process can be started by calling one of the methods `Dirigent#compose(Context, String, Object...)` or `Dirigent#compose(String, Object...)`. The latter one will create an empty context and call the first method. The process consists of three independent steps. The first one divides the message into tokens with a Tokenizer. Here are two different types. Text tokens representing static text and macros which must be processed. 

The next step converts this tokens into components. Here mainly the macros are converted into a resolved macro or an unresolved macro component. The resolved macro component represents a macro together with the formatter to use and the input parameter to format. Formatter can be registered at the Dirigent instance using `Dirigent#registerFormatter(Formatter)`. To load the correct formatter for a macro, a formatter has a method `Formatter#names` returning a set of names of a macro triggering this formatter. Additionally the `Formatter#isApplicable(Object)` method is used to check whether the formatter is able to handle the type of the message input parameter to format. If a macro doesn't have a name, a default formatter will be used which was specified at Dirigent creation time. By default it is the `StringFormatter`, which is described below. This default formatter must handle all object types. The `Formatter#isApplicable(Object)` method is not checked at this point! A token will be converted into an unresolved macro component if a converter couldn't be found. This can have two reasons. The first reason is that there isn't any registered formatter handling the used name of the macro. The second one represents the case that there is a formatter for the macro, but it doesn't handle the actual type of the message input parameter. Both reasons are represented with a `MacroResolutionState` having one of the values `UNKNOWN_NAME` or `NONE_APPLICABLE`. After converting a token to a component, the registered `PostProcessor`s of the `Dirigent` instance will be called. They're allowed to manipulate the components. More about it can be found in the PostProcessor section of this documentation. All the components will be grouped in a component group.

The first two steps are done by the `AbstractDirigent` implementation. The last step is only triggered by it and must be implemented in a sub class. Here the components must be composed to the actual message. The Dirigent framework provides the `BuilderDirigent` implementation using a `MessageBuilder` to compose the final message. This builder has two generic types. The type of the actual message and the type of the builder to use. The `StringMessageBuilder` composes `String` messages using a `StringBuilder`. The components of a component group will be loaded and processed individually. The text of Text components are appended without any modification. Resolved macro components are converted to another component by calling the actual formatter. Unresolved macro components are appended as a `{{unresolved: <macro-name>}}` string. All other kind of componants will result in an IllegalStateException. To change this behaviour the responsible methods can be overwritten. In the end the final message object will be returned.

# Context

The dirigent process can be started with a special compose context. This context includes information for the formatter and post processor which can be evaluated by them. The context is expandable dynamically. Specific entries relate to a specific `ContextProperty`. This framework provides entries for a `Locale`, a `TimeZone` and a `Currency` within the static context of the `Contexts` helper class. Every `ContextProperty` contains a `DefaultProvider` which is used for getting a default value of the property if it isn't specified. To create a `PropertyMapping` which is necessary to create a compose context, the method `ContextProperty#with(T)` can be used. The creation of a new context should be done by using the `Contexts` class. Besides a few properties it provides methods for creating a context.

# Formatter

Formatter are needed to format the messages input parameters. By default the Dirigent instance only has a default formatter, but macros having a name can't be processed. Instead the Dirigent instance has a method called `registerFormatter(Formatter)` which must be used to register a formatter formatting macros with specific names. Furthermore the default formatter can be overwritten by providing it at the Dirigent constructor.

`Formatter` is an abstract class providing the functionality to handle post processors already. Every implementation must implement the method `names` returning a set of strings representing the macro names which can be handled with the formatter, the method `isApplicable(Object)` returning the inforation whether the specified object can be handled by this formatter and the method `format(T, Context, Arguments)` returning a component representing the actual formatting result. The T parameter represents the messages input parameter which shall be format. Context is the compose context and Arguments contains the arguments of the macro. The available implementations `AbstractFormatter` and `ReflectedFormatter` are little helpers implementing a little functionality already. The `AbstractFormatter` can be used to handle a specific object type like `Integer` or `Date`. The object type is read from the generic type of the class which is used for the implementation of the `isApplicable(Object)` method. Furthermore a method for `names` exists as well. The names must be provided as constructor parameters. The `ReflectedFormatter` uses annotations to get the details. An implementation class must have the `Names` annotation at the class definition. Additionally it can provide several format methods having an object parameter and optionally a compose context and an arguments object. The methods must be marked with the `Format` annotation. The `ReflectedFormatter` checks the input parameter types and looks for the format implementation to use at runtime.

In addition to the described formatter, there are also `ConstantFormatter`. A constant formatter is special formatter type which doesn't consume any message input parameter. Instead it only uses the context and the macro arguments to format something. 

## Available Formatters

The Dirigent framework provides a few formatters already. They all provide default macro names already. Those can be changed by providing new names at the constructor.

### StringFormatter

The `StringFormatter` can be used to format any object by calling the `String#valueOf(Object)` method. This formatter is also used as the default formatter. It is possible to provide one of the flags `uppercase` or `lowercase` to return the uppercase or lowercase version of the string. The formatter only has one default name which is `string`.

**Example:**

- `dirigent.compose("Hello {string}", "Stefan")` will result in `Hello Stefan`
- `dirigent.compose("Hello {string:uppercase}", "Stefan")` will result in `Hello STEFAN`

### NumberFormatter

The `NumberFormatter` can be used to format any `Number` object. This is done by using a `NumberFormat`. By default this class uses the `NumberFormat#getInstance` method to format the number. 

This behaviour can be changed by providing arguments. The parameter `format` lets you specify an individual format which creates a `DecimalFormat` instance. Additionally it's possible to specify one of the flags `integer`, `currency`, `percent`. These result in of the methods `NumberFormat#getIntegerInstance`, `NumberFormat#getCurrencyInstance` and `NumberFormat#getPercentInstance`.

Every instance will be created with a the value of the property `Contexts.LOCALE` which is specified in the compose context. Furthermore the value of `Contexts.CURRENCY` will be considered and used as the `Currency` of the `NumberFormat`.

In addition it's possible to change the default behaviour of this formatter by specifying one of the modes `INTEGER`, `CURRENCY` and `PERCENT` at the constructor. This causes that it represents the respective flag automatically. 

The default names are `number`, `decimal`, `double` and `float`.

**Example:**

- `dirigent.compose("Result: {number}", 12345.344)` will result in `Result: 12,345.344` with Locale `en-US`
- `dirigent.compose("Result: {number:format=#,###.0}", 36.4567)` will result in `Result: 36.5` with Locale `en-US`
- `dirigent.compose("Result: {number:integer}", 36.4567)` will result in `Result: 36` with Locale `en-US`
- `dirigent.compose("Result: {number:currency}", 12345)` will result in `Result: $12,345.00` with Locale `en-US`
- `dirigent.compose("Result: {number:currency}", 12345)` will result in `Result: EUR12,345.00` with Locale `en-US` and Currency Euro
- `dirigent.compose("Result: {number:percent}", 0.12)` will result in `Result: 12%` with Locale `en-US`

#### IntegerFormatter

The `IntegerFormatter` overwrites the `NumberFormat` and sets the mode to `INTEGER`. The default names are `integer`, `long`, `short`, `amount` and `count`.

**Example:**

- `dirigent.compose("Result: {integer}", 36.4567)` will result in `Result: 36` with Locale `en-US`

#### CurrencyFormatter

The `CurrencyFormatter` overwrites the `NumberFormat` and sets the mode to `CURRENCY`. The default names are `currency`, `money` and `finance`.

**Example:**

- `dirigent.compose("Result: {currency}", 12345)` will result in `Result: $12,345.00` with Locale `en-US`
- `dirigent.compose("Result: {currency}", 12345)` will result in `Result: EUR12,345.00` with Locale `en-US` and Currency Euro

#### PercentFormatter

The `PercentFormatter` overwrites the `NumberFormat` and sets the mode to `PERCENT`. The default names are `percent` and `percentage`.

**Example:**

- `dirigent.compose("Result: {percent}", 0.12)` will result in `Result: 12%` with Locale `en-US`

### DateTimeFormatter

The `DateTimeFormatter` can be used to format a `Date` object. This is done by using a `DateFormat`. The behaviour of this class must be specified using one of the modes `DATE_TIME`, `DATE` or `TIME`. The first mode is responsible for formatting a date to a date and the time. The second mode only returns the date part and the last mode represents the time. In contrary to the `NumberFormatter` here the mode must be specified. By default the `DATE_TIME` mode is used.

The `DATE_TIME` mode loads the formatter with the `DateFormat.getDateTimeInstance` method. As styles it uses the default style. Of course this can be changed by specifying parameters. Does the macro contains one of the flags `short`, `medium`, `long` or `full` the related style will be used for both the date and the time. Additionally it's possible to style the two date parts separately. This can be done with the parameters `date` and `time` with a value which is similar to the flags.

The other two modes `DATE` and `TIME` result in a `DateFormat` loaded with `DateFormat.getDateInstance` or `DateFormat.getTimeInstance`. The macro arguments work the same way. Of course here it doesn't make sense to provide one of the parameters `date` and `time`, the flag is enough, nevertheless it's possible.

Furthermore the `format` parameter is supported as well. The format results in a respective `SimpleDateFormat` which doesn't care about the actual mode of the formatter.

Every instance will be created with a the value of the property `Contexts.LOCALE` which is specified in the compose context. Furthermore the value of `Contexts.TIMEZONE` will be considered and used as the `TimeZone` of the `DateFormat`.

The formatter only has one default name `datetime`.

**Example:** assuming the date object represents May 25, 2017 3:13:21 PM UTC

- `dirigent.compose("Result: {datetime}", date)` will result in `Result: May 25, 2017 3:13:21 PM` with Locale `en-US`
- `dirigent.compose("Result: {datetime}", date)` will result in `Result: May 25, 2017 5:13:21 PM` with Locale `en-US` and time zone `Europe/Berlin`
- `dirigent.compose("Result: {datetime:full}", date)` will result in `Result: Thursday, May 25, 2017 3:13:21 PM UTC` with Locale `en-US`
- `dirigent.compose("Result: {datetime:long}", date)` will result in `Result: May 25, 2017 3:13:21 PM UTC` with Locale `en-US`
- `dirigent.compose("Result: {datetime:medium}", date)` will result in `Result: May 25, 2017 3:13:21 PM` with Locale `en-US`
- `dirigent.compose("Result: {datetime:short}", date)` will result in `Result: 5/25/17 3:13 PM` with Locale `en-US`
- `dirigent.compose("Result: {datetime:date=short:time=medium}", date)` will result in `Result: 5/25/17 3:13:21 PM` with Locale `en-US`
- `dirigent.compose("Result: {datetime:short:time=medium}", date)` will result in `Result: 5/25/17 3:13:21 PM` with Locale `en-US`
- `dirigent.compose("Result: {datetime:format=YYYY.MM.dd}", date)` will result in `Result: 2017.05.25` with Locale `en-US`

#### DateFormatter

The `DateFormatter` overwrites the `DateTimeFormatter` and sets the mode to `DATE`. The default name is `date`.

**Example:**

- `dirigent.compose("Result: {date}", date)` will result in `Result: May 25, 2017` with Locale `en-US`
- `dirigent.compose("Result: {date:short}", date)` will result in `Result: 5/25/17` with Locale `en-US`
- `dirigent.compose("Result: {date:format=HH:mm:ss}", date)` will result in `Result: 15:13:21` with Locale `en-US`

#### TimeFormatter

The `TimeFormatter` overwrites the `DateTimeFormatter` and sets the mode to `TIME`. The default name is `time`.

**Example:**

- `dirigent.compose("Result: {time}", date)` will result in `Result: 3:13:21 PM` with Locale `en-US`
- `dirigent.compose("Result: {time:short}", date)` will result in `Result: 3:13 PM` with Locale `en-US`
- `dirigent.compose("Result: {time:format=YYYY.MM.dd}", date)` will result in `Result: 2017.05.25` with Locale `en-US`

### StaticTextFormatter

The `StaticTextFormatter` is a constant formatter which doesn't consume an input parameter. Instead it only writes the text of the first argument directly to the message. This could be used to indicate text parts which shouldn't be formatter for example. The default name of the formatter is `text`.

**Example:**

- `dirigent.compose("The value of the property is {text:true}")` will result in `The value of the property is true`
- `dirigent.compose("This framework is {text:incredible awesome}")` will result in `This framework is incredible awesome`

Pro tip: With a post processors which is described in the next section this static text can be styled in a special example. For example it could be displayed bold or italic. 

# Post Processors

A post processor can be used to manipulate a macro after it was created by a formatter or by the dirigent instance. Therefore the interface `PostProcessor` provides a method `process(Component, Context, Arguments)` getting the created component, the current compose context and the arguments of the macro. The result of the method is a component again. The input component will be replaced with the output component. They're allowed to be the same object of course.

A post processor can be registered using one of the methods `Dirigent#addPostProcessor(PostProcessor)` or `Formatter#addPostProcessor(PostProcessor#)`.

In a post processor or a formatter it is possible to return own component implementations. Just note that own implementations must be handled in the `MessageBuilder` class by yourself. So you've to overwrite it. Instead using a `TextComponent` (or implementation `Text`) or a `ComponentGroup` might be enough as well.
