Long lists of parameters are tedious. The builder pattern improves code clarity by making each parameter name visible.

In many common implementations of the builder pattern, such as those generated by [auto-value](https://github.com/google/auto/tree/master/value), it is possible for the user to specify an argument twice, or forget a required argument. In the second case, an implicit default value of `null` is then used, and optionally rejected at runtime.

Another option is to have a chain of dedicated classes, each representing one parameter. In functional programming terms, this is somewhat similar to _currying_. By taking advantage of how interfaces work in java, a single mutable object can be used for each step of this chain. In fact, we can even store that object in a `ThreadLocal`, to keep the garbage collector happy.
