for /l %%x in (1, 1, 100) do gradlew clean test -Dtest.single=ShittyPerformanceTest > output/%%x.txt
