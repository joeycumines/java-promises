# java-promises - Writing async code to the style of JavaScript Promises

I kinda just sat down and wrote this, tests are on the to do list so ah it is probably completely broken

TODO: write this readme

## JavaDoc
[Read the API documentation HERE](https://joeycumines.github.io/java-promises/)


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

This one was interesting. The single threaded solution obviously had very variable cost, but still managed to
outperform all others, which I can only assume is a combination of the type of task, and not having the overhead in
creating objects. If I had run it with a considerable deeper tree, then there would probably be more benefit to
multiple threads. It is also probably related to the fairly decent single core speed of the processor used.

The most interesting part of this was how well `CompletableFuture` performed, considerably more then my multi threaded
implementation, though that could be considered a side effect of using a fixed thread pool of `40` for the multi 
threaded test (I have run a few re-tests on that one, it seems using the fork join common pool makes it much quicker).
If I had to guess, I would say that the difference in speed is due to the cost of the promise implementation's general
implementation; if you look at code for the future test, in `MazeTester`, the `CompletableFuture` control logic is
very direct. While the `PromiseApi.any` implementation used for all promise tests does support early exit, it is very
likely sub-optimal for this particular case, where there is no internal cost in compute time to even the odds. The
promise api must go through quite a few more callbacks, objects, and threads for each one of the future implementation.

The promise results are below, I will let you draw your own conclusions. Both my implementation, and the implementation
using the j8stage repo's `MyFuture` class came out on top, with mine edging ahead. Following the same line of inquiry
as before, this result supports the conclusion that the cost is likely contributed to one or all of the number of
callbacks, threads, or objects created, as the `PromiseStage` implementation necessitated less direct logic routes, if
it was to encapsulate any `CompletionStage` while providing the same `Promise` functionality.

### The combined results of 100 consecutive (separate), somewhat primitive benchmarks
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
