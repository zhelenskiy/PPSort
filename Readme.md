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
Initializing...
Heating...
Benchmark:
Seq: 35.508525938s, Par: 15.944720942s, Ratio: 2.226976945357944
Seq: 36.008090906s, Par: 14.153748159s, Ratio: 2.544067514943269
Seq: 35.531662696s, Par: 14.517148706s, Ratio: 2.447564836290105
Seq: 35.493249508s, Par: 15.170966484s, Ratio: 2.339550980185265
Seq: 35.899818041s, Par: 15.214200312s, Ratio: 2.3596256986760253

Average ratio: 2.3835571950905217
```

![Screen](bfs.png "Illustration")