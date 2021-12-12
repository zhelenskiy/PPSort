My sort output is:
```yaml
seq #1: 52347.335353ms
par #1: 16662.998798ms
seq #2: 50442.947059ms
par #2: 15235.085273ms
seq #3: 54896.307276ms
par #3: 14930.354696ms
seq #4: 46743.352704ms
par #4: 17166.486762ms
seq #5: 51088.333968ms
par #5: 14947.330394ms
Sequential:	51103.655271999996ms
Parallel:	15788.4511846ms
Rate:	3.236774441931728
```
Every even row is temporary parallel or sequential result time.

![Screen](sort.png "Illustration")

---

My BFS output is:
```kotlin
Seq: 50.642763700s, Par: 25.580496146s, Ratio: 1.9797412611138492
Seq: 53.194839704s, Par: 21.941604628s, Ratio: 2.4243823825043904
Seq: 53.295811907s, Par: 23.064672718s, Ratio: 2.3107118214344826
Seq: 50.923652227s, Par: 22.843449095s, Ratio: 2.2292453304762208
Seq: 50.480597217s, Par: 23.333747158s, Ratio: 2.1634157975219455

Average ratio: 2.221499318610178
```

![Screen](bfs.png "Illustration")