# java-promises - Writing async code to the style of JavaScript Promises
After deciding to re-learn Java, and without any real knowledge of most Java 8 features but coming from using JavaScript
frequently, I was very interested to learn of the `CompletableFuture` and the related `CompletionStage` interface.
When my eyes started hurting immediately after opening the JavaDoc however, I decided it would be a valuable learning
experience to implement my own, more friendly implementation.

I wrote this library with the goal of making it something I would be happy to use in production code, and to that end I
focused on designing flexible, sensible interfaces, that were well documented and thought out and as simple as they
could be, while delivering the functionality I wanted. It's also very close to 100% tested, though I did frequently
sacrifice test clarity and readability for overall thoroughness. The design is based quite heavily on the ES6 JavaScript
Promise, with changes where I saw fit.

This project has taught me a huge amount about generics, thread safety, the JVM, mocks in Java, along with many other
things, and I am very happy with the current design. I will be using this library in another project, and making any
improvements to this library that I think of - I don't believe I will be changing the interfaces.

## Performance?
Pretty good actually. The benchmarks are low-tech but they were very enlightening, to give a tl;dr:

- The biggest factor in performance for a given problem is the type of threading, e.g. the `Executor` used
- My initial `Promise` implementation is on par with `CompletableFuture` in terms of performance (variable depending 
on the use case), but imo it works far better for blocking logic due to it's design, as how promises are run is much
easier and less tedious to configure, and much more flexible out of the box.
- If you replace what could be simple logic with complicated thread switching logic, you're going to have a bad time,
but it's the same situation with `CompletableFuture`... `Promise` just makes it much easier to do
- If you are cognisant of the fact that chaining methods like `Promise.then()` ALL run async (like the async suffixed
methods of `CompletionStage`) your code should be performant, if not you are probably using this library wrong.

## Interoperability?
Use promises within completion stages, write your own promise implementation, use your completion stage implementation
as a promise, use different promise implementations together, configure how separate promises are run, go for your life.

Though the promises are designed to take advantages of generics in a inherently type safe way, I wrote an implementation
of chained resolution to an unknown depth, in the same manner as JS promises, provided by `PromiseApi.resolve()`, 
which can also detect circular references (something which is not necessary if types are used properly).

## What's the structure look like?
Pretty much you want to look at the interfaces `Promise` and `PromiseFactory`, and you probably want to check out the
abstract class `PromiseApi` (that requires a `PromiseFactory` implementation), but the api is entirely optional.

## JavaDoc
[Read the API documentation HERE](https://joeycumines.github.io/java-promises/)

## Changelog
- 1.0.0 - Initial Release
    - 1.0.1 - minor change for consistent return types in PromiseApi

## (Bad) Benchmark - 100x Mean + STDDEV (Windows 10 x64)
Out of interest, I implemented some basic benchmarks (using `System.currentTimeMillis()`), which can be found and run 
via the `ShittyPerformanceTest` class, using JUnit. In an effort to make it slightly more scientific, I wrote controls
for each test, single threaded, and using `CompletableFuture` and/or basic multi threading, and tested 4 separate
implementations of my `Promise` and related interfaces, including my initial `PromiseRunnable`, and 3 others that all
use `PromiseStage`, a wrapper for `CompletionStage`, which allowed me to test against (in a possibly unfair way),
the `CompletableFuture` class, and two other libraries I found on GitHub, 
[lukas-krecan/completion-stage](https://github.com/lukas-krecan/completion-stage) and
[chrisalice/j8stages](https://github.com/chrisalice/j8stages).
Each of these promise tests were run with two executors, a cached thread pool, and a common fork join pool, for a total
of 8 separate iterations of each test, iterations which were run in a randomized order each time, and each time worked 
from the same randomized inputs (which the controls also worked from). Promise test codes starting with `3` and `4`
are using the `CompletableFuture` as a base.

The tests cases are each one of two scenarios, a brute force tree search, and a request-response style async test, 
both which are contrived, but which have provided some interesting results. I tried to keep the setup tasks consistent
and if possible out of the actual time, for each iteration of each test, be it promise or control.

The test case was run 100 times using windows batch scripting (I originally ran it on Ubuntu using OpenJDK, but I felt
a Windows desktop would be a more fair test), output to text files, then combined using a script I wrote for the
purpose. The result below was generated from console output, where, after some line normalization, any changed numeric 
values where first collated, processed, then re-inserted into their original positions, in the format `|mean (stddev)|`.
In terms of the relative performance, running it using OpenJDK had consistent results with this test.

### "Maze" benchmark - brute force tree search
This test was the first one I wrote, and likely more naive. It consists of a tree structure where each parent has an
array of child nodes, where the algorithm must find the success leaf node, which is selected pseudo-randomly. To prove
it has found the success case, the algorithm must find the combined string of each unique node id, in the path from the
start to the finish.

#### Single threaded control test
The single threaded solution obviously had very variable cost, but still managed to outperform all others, which I can 
only assume is a combination of the type of task, and not having the overhead in creating objects. If I had run it with 
a considerably deeper tree, then there would probably be more benefit to multiple threads. It is also probably related 
to the fairly decent single core speed of the processor used.

#### CompletableFuture and simplistic multi threaded control tests
The `CompletableFuture` control test performed the best out of the multi threaded solutions, for reasons I mostly 
attribute to poor test design. The multi threaded control implementation lost by a large margin, however that would seem
to be a side effect of using a fixed thread pool of `40`, having run a few re-tests, it seems using the fork join common
pool makes it much quicker.

The `4(breadth) x 11(depth)` maze test was the most enlightening, as all `Promise` implementations performed much worse
in comparison to the controls. There are a few likely reasons for this, and if I had to guess, I would say that the 
difference in speed is due to the cost of the promise implementation's more general implementation; if you look at code 
for the future test, in `MazeTester`, the `CompletableFuture` control logic is very direct. While the `PromiseApi.any`
implementation, that was used for all promise tests, does support early exit, it was very likely sub-optimal for this 
particular case, where there is no internal cost in compute time to even the odds. The promise api must go through quite
a few more callbacks, objects, and threads for each one of the future implementation.

##### After further testing:
Refactoring the `Promise` test case to more closely match the one for `CompletableFuture` saw the test time decrease for
the `4(breadth) x 11(depth)` test by over a 100% across the board, which would support the previous conclusions. This
change saw `1_RUNNABLE` come last in several tests of the `10(breadth) x 6(depth)` maze, a test where it's fork-join
equivalent as well as the `MyFuture` promise came out on top, and which was the quickest test overall.

#### The Promise implementation tests
Both my original implementation, and the implementation using the j8stage repo's `MyFuture` class came out on top, 
with mine edging ahead. Following the same line of inquiry as before, this result supports the conclusion that the cost
of this test is greatly affected by the number of callbacks, threads, or possibly objects created, as the `PromiseStage` 
implementation necessitated less direct logic routes, if it was to encapsulate any `CompletionStage` while providing the
same `Promise` functionality.

### Request-Response test - Interpreting strings as an equation
Using a cool little algorithm I grabbed off StackOverflow, this test emulates a request-response style problem, with
adjustable and consistent cost of work, and the ability to define the number of requests. This test supports the
conclusion that the overhead involved with even a very large number of promises matters very little when more processing
intensive tasks become more relevant. The test with the most costly operation `difficulty 10000 and 1000 connections`,
which result in a work time (per request) averaging on the low end of 10ms, showed that all the asynchronous approaches
were on par, as there was no significant difference between any of the methods using the fork join common pool.

It can also be concluded that the most expensive part of the `Promise` implementation probably comes when using a large
number of dependant promises, to perform complicated logic. The `difficulty 100 and 100000 connections` test supports
this as well; the `Promise` implementations perform effectively the same as the `CompletableFuture` control, with only
relatively small variations that can be correlated with the type of `Executor` used, and the style of implementation
backing it.

### The combined results of 100 consecutive (separate), somewhat primitive benchmarks
The results discussed above. It's worth noting that, for the test with the greatest disparity between 
`CompletableFuture`, and any of the `Promise` implementations, adjusting the `Promise` test so that it more closely
matched the future control resulted in a considerable time decrease, such that when run on a Thinkpad T450s, using 
OpenJDK on Ubuntu 16.04, the `1_RUNNABLE` test case consistently beat the `CompletableFuture` control, for the same 
`4(breadth) x 11(depth)` maze test.

```
    --generating maze for 10(breadth) x 6(depth)--
    --maze generation complete--
    [control] maze solution single threaded took (ms): |11.37 (4.85)|
    [control] maze solution multi threaded took (ms): |80.65 (16.6)|
    [control] maze solution using CompletableFuture took (ms): |82.29 (27.29)|
    RUNNING: maze test
    COMPLETED: maze test
    #|1.92 (1)| 1_RUNNABLE ( |79.76 (29.76)| ms )
    #|2.18 (1.07)| 2_RUNNABLE_FORK_JOIN_COMMON ( |86.91 (32.17)| ms )
    #|5.76 (0.85)| 3_STAGE ( |294.17 (40.09)| ms )
    #|5.71 (0.83)| 4_STAGE_DEFAULT_EXECUTOR ( |294.66 (40.03)| ms )
    #|7.13 (0.77)| 5_JAVACRUMBS ( |358.48 (40.8)| ms )
    #|7.4 (0.79)| 6_JAVACRUMBS_FORK_JOIN_COMMON ( |373.84 (44.04)| ms )
    #|2.94 (0.97)| 7_MYFUTURE ( |120.23 (44.07)| ms )
    #|2.96 (1.05)| 8_MYFUTURE_FORK_JOIN_COMMON ( |116.28 (41.08)| ms )
    --generating maze for 4(breadth) x 11(depth)--
    --maze generation complete--
    [control] maze solution single threaded took (ms): |72 (40.57)|
    [control] maze solution multi threaded took (ms): |936.27 (366.66)|
    [control] maze solution using CompletableFuture took (ms): |385.92 (303.12)|
    RUNNING: maze test
    COMPLETED: maze test
    #|1.95 (1.08)| 1_RUNNABLE ( |1921.45 (316.7)| ms )
    #|2.05 (1.12)| 2_RUNNABLE_FORK_JOIN_COMMON ( |1953.71 (339.02)| ms )
    #|5.69 (0.87)| 3_STAGE ( |5259.57 (455.88)| ms )
    #|5.63 (0.75)| 4_STAGE_DEFAULT_EXECUTOR ( |5220.8 (385.25)| ms )
    #|7.26 (0.66)| 5_JAVACRUMBS ( |6097.08 (439.79)| ms )
    #|7.42 (0.65)| 6_JAVACRUMBS_FORK_JOIN_COMMON ( |6183.01 (407.21)| ms )
    #|3.07 (0.83)| 7_MYFUTURE ( |2166.44 (422.55)| ms )
    #|2.93 (0.96)| 8_MYFUTURE_FORK_JOIN_COMMON ( |2142.81 (331.68)| ms )
    -- Running the Request-Response test for difficulty 10000 and 1000 connections
    -- generating requests
    -- requests generated
    [control] on average the actual work should take (ms): 1
    [control] a single thread served all requests sequentially in (ms): |1660.54 (70.49)|
    [control] for CompletableFuture on average the work took (ms): |2.85 (0.41)|
    [control] for CompletableFuture the total time spent was (ms): |462.66 (39.72)|
    RUNNING: MathRequester test
    COMPLETED: MathRequester test
    #|5.66 (1.43)| 1_RUNNABLE ( |499.21 (29.37)| ms )
    #|3.45 (2.45)| 2_RUNNABLE_FORK_JOIN_COMMON ( |477.07 (40.88)| ms )
    #|6.01 (1.47)| 3_STAGE ( |503.48 (28.95)| ms )
    #|2.27 (1.36)| 4_STAGE_DEFAULT_EXECUTOR ( |460.72 (24.35)| ms )
    #|6.08 (1.65)| 5_JAVACRUMBS ( |507.98 (29.94)| ms )
    #|4.05 (2.28)| 6_JAVACRUMBS_FORK_JOIN_COMMON ( |484.1 (45.36)| ms )
    #|5.93 (1.5)| 7_MYFUTURE ( |504.14 (29.24)| ms )
    #|2.55 (1.34)| 8_MYFUTURE_FORK_JOIN_COMMON ( |463.19 (26.05)| ms )
    [result] MathRequester time for 1_RUNNABLE was (ms): |8.06 (9.1)|
    [result] MathRequester time for 2_RUNNABLE_FORK_JOIN_COMMON was (ms): |2.99 (0.17)|
    [result] MathRequester time for 3_STAGE was (ms): |5.29 (3.42)|
    [result] MathRequester time for 4_STAGE_DEFAULT_EXECUTOR was (ms): |2.96 (0.2)|
    [result] MathRequester time for 5_JAVACRUMBS was (ms): |8.91 (11.24)|
    [result] MathRequester time for 6_JAVACRUMBS_FORK_JOIN_COMMON was (ms): |2.98 (0.28)|
    [result] MathRequester time for 7_MYFUTURE was (ms): |5.8 (3.71)|
    [result] MathRequester time for 8_MYFUTURE_FORK_JOIN_COMMON was (ms): |2.99 (0.17)|
    -- Running the Request-Response test for difficulty 100 and 100000 connections
    -- generating requests
    -- requests generated
    [control] on average the actual work should take (ms): 0
    [control] a single thread served all requests sequentially in (ms): |1724.58 (114.75)|
    [control] for CompletableFuture on average the work took (ms): 0
    [control] for CompletableFuture the total time spent was (ms): |586.15 (56.87)|
    RUNNING: MathRequester test
    COMPLETED: MathRequester test
    #|5.8 (1.63)| 1_RUNNABLE ( |691.48 (115.13)| ms )
    #|2.04 (1.8)| 2_RUNNABLE_FORK_JOIN_COMMON ( |586.4 (82.8)| ms )
    #|5.5 (1.93)| 3_STAGE ( |694.54 (101.17)| ms )
    #|2.7 (1.68)| 4_STAGE_DEFAULT_EXECUTOR ( |606.2 (83.41)| ms )
    #|5.75 (1.74)| 5_JAVACRUMBS ( |689.86 (83.22)| ms )
    #|5.08 (1.61)| 6_JAVACRUMBS_FORK_JOIN_COMMON ( |667.62 (96.48)| ms )
    #|5.8 (1.96)| 7_MYFUTURE ( |715.29 (128.47)| ms )
    #|3.33 (1.85)| 8_MYFUTURE_FORK_JOIN_COMMON ( |629.27 (104.64)| ms )
    [result] MathRequester time for 1_RUNNABLE was (ms): 0
    [result] MathRequester time for 2_RUNNABLE_FORK_JOIN_COMMON was (ms): 0
    [result] MathRequester time for 3_STAGE was (ms): |0.01 (0.1)|
    [result] MathRequester time for 4_STAGE_DEFAULT_EXECUTOR was (ms): 0
    [result] MathRequester time for 5_JAVACRUMBS was (ms): 0
    [result] MathRequester time for 6_JAVACRUMBS_FORK_JOIN_COMMON was (ms): 0
    [result] MathRequester time for 7_MYFUTURE was (ms): 0
    [result] MathRequester time for 8_MYFUTURE_FORK_JOIN_COMMON was (ms): 0
Total time: |53.13 (1.27)| secs
```

## License - SEE LICENSE
Copyright 2017 Joseph Cumines

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
