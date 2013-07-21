[UniteD] InnoVatioN: Entry for 2013 International GGP Competition 
=================================================================

# Overview
My GGP player is inspired by the Starcraft 2 player STX SouL INoVation. Like INoVation himself, [UniteD] InnoVatioN isn't really innovative at the higher level strategy level. Instead, I took the approach of implementing the "de facto" standard strategy of UCT and trying to make it really fast.

# Dependencies
This player is built on top of the fantastic Rekkura GGP framework (<https://github.com/ptpham/rekkura>), which is actively developed and maintained by Peter Pham (<https://github.com/ptpham>).
I've added my own utilities, so I've packaged up a Rekkura jar with this repo.  

# Setup
TODO

# PropNet
Standard players usually take the route of compiling a GDL ruleset into a 
Propositional Network (abbv. PropNet) and then using the PropNet as a game state machine.  
A PropNet is a circuit composed of logic gates (And, Not, Or).  
It's a very brute force technique- in the naive approach, each grounding of a term in a rule must have it's own component in the PropNet.  
Here's a concrete example:
Consider a rule p(X,Y) <= q(X,A) /\ r(Y,B) where X,Y,A,B are variables
with the corresponding groundings: X = {x}, Y = {y}, A = {a0,a1}, B = {b}.
The term p(X,Y) is the head of the rule and q(X,A) /\ r(Y,B) is a body (with
terms q(X,A) and r(Y,B) are body terms).
To do inference, we want to know if the grounding p(x,y) is true.
This corresponds to evaluating the following logical statement: <br/>
p(x,y) <= ( q(x, a0) /\ r(y, b0) ) | ( q(x, a0) /\ r(y, b1) ) | ( q(x, a1) /\ r(y, b0) ) | ( q(x, a1) /\ r(y, b1) )   <br/> 
As can be seen, there's a grounding of the body for each variable assignment 
that needs to be evaluated. 

## Generation
I initially took a pretty naive approach to PropNet generation.
Here's the basic algo: 
0. Generate components for the grounds initially true. <br/> 
1. Generate a topological ordering R on the rules <br/>
2. For each rule r in R: <br/>
3. For each grounding g of the rule r: <br/>
4. Make an OR gate for the head of g. <br/>
5. Add an AND gate as input the OR gate <br/>
6. For each term in the body of g, add it's corresponding component as
    input to the AND gate <br/> 

Luckily, Rekkura provides library functions that make steps 0, 1 and 3
relatively easily. As packaged, however, step 3 was prohibitively slow for certain games.

To deal with this, I wrote my own method for step 3, which is contained in 
Grounder. It leverages a data structure I call a Filtering Cartesian Iterator,
which is a generalization of a Cartesian Iterator.

Given a set of subspaces to iterate over, a Cartesian Iterator just iterates
over the Cartesian product of them. So give subspaces {1,2} and {3,4}, the Cartesian Product would be {(1,3), (1,4), (2,3), (2,4)}.

Cartesian Iterators are typically implemented by maintaining a running
assignment to a point in the product space (ie. after iterating returning (1,3) when next() is called, the iterator may store (1) as its running assignment before returning (1,4) on the next next() call)

A Filtering Cartesian Iterator lets you exclude even incomplete assignments
based on a filtration function you pass in to it. For the example above, suppose we wanted to require that the second term not be odd. Then we could pass in a filtration function like this: </br>
fn (running, current) -> ( return !(running.size() == 1 && current % 2) )</br>
and that should give an iterator over {(1,4),(2,4)}.

Using the Filtering Cartesian Iterator with a somewhat gnarly filtration function,
I was able to do step 3 pretty fast.

Implementation details are in logic/Grounder.java and
propnet/util/FilteringCartesianIterator.java.
 
## Optimizations
Generally, a PropNet with fewer components will be faster to do inference on. At the generation level, the only optimization I implemented was DeMorgan simplification. This is when ((!a)/\(!b)/\(!c)) is converted to !(a | b | c). Since NOT gates cost an extra component, the number of gates needed decreases from 4 to 2.

### Native Compilation
By far the biggest performance optimization is compiling the PropNet to a bit vector representation and then using the bit manipulations to advance state. Since a component in a PropNet just needs to be either true or false, there's no point in having an entire Java object for it. This optimization is done by hardcoding the state advancements in its own source file. 

Last year, my team took the approach of having the bit vector in Java. However, we ran into the issue that Java class size limit is 64KB, and some games can easily exceed this. To get around this problem, I just used JNI.  To make this work for yourself, you'll have to add 'compiled/src' to the java.library.path. 

# Player Implementation
## Multithreading
Without any parallelization, InnoVatioN ran with ~25% cpu utilization.
I figured there was ample opportunity to increase performance. 
PropNet generation and inference can't really be parallelized because they do proccessing in topological order.  Furthermore, a single UCT playout + stats aggregation can't really be parallelized either because the playouts are sequential. Thus, the playouts themselves need to be done in parallel. 

I tried two strategies for this. My first attempt was to have multiple threads run playouts in parallel and then update a shared cache to record the stats.
Unfortunately, this didn't increase the cpu utilization at all and ended up making the player even slower (about a 40% decrease in playouts per second on connect 4).  

My second attempt was to have multiple threads run playouts in parallel but also maintain their own stats caches.  When the allotted time ran out, the main player thread would accumulate the results and choose its move from the accumulated cache. Implementing this turned out to be an exercise in ExecutorServices, so I got to learn that interface well.

The second approach ended up being a lot more fruitful.  I was able to achieve close to 100% cpu utilization.
  
## Kaskade: Smooth State Machine Updates
A GGP match is composed of a metagaming period before the match (called the startclock) and then each successive turn has its own time limit (called the playclock).
The way I structured things, the state machine construction forms the following dependency chain: <br/> Prover State Machine -> PropNet Generation -> Native Compilation. <br/> There is about an order of magnitude increase in charges per second when going from one type of state machine to the next, so it's best to take the machine that's furthest along in this chain. 

Building a Prover State Machine will finish within the startclock bounds, but PropNet Generation and Native compilation will often exceed that.  If the player thread builds the state machine itself, it will be hosed until it finishes.  It's a much better idea to delegate the state machine build tasks to separate threads. 

At the beginning of each turn, the player will update its state machines to the best ones that have finished building. To accomodate multiple depth charge threads, when a certain state machine finishes building, copies of this machine will be made for the threads.  This needs to happen because of the dob caching that takes place for the state machines.  

The anatomy of a turn ended up looking like this (according to relative
duration): <br/>
|-|--------------------------------------------------|-------------|-------| <br/>
 0                        1                                 2          3 <br/>

0: update state machines </br>
1: drop UCT depth charges </br>
2: accumulate the results and pick a decision </br>
3: fuzz padding  </br>

I also introduced a Config abstraction which allows you to, easily set parameters of the player (number of threads, depth charge time, accumulation time, fuzz, state machine "kaskade" level).  The Config actually takes care of much of the heavy lifting for setting up the player. 

For more implementation details, see player/uct/ConfigInterface.java and player/uct/ConfigFactory.java  

# Competition Recap
Unfortunately, even though I passed the player connectivity test, the competition hosts couldn't send me any games. Thus, InnoVatioN was disqualified on day 1. C'est la vie :)

You can find a recap of the competition here: 
<http://gamemaster.stanford.edu/showmatches.php?tournament=iggpc2013>

# Acknowledgements
I would like to thank Peter Pham for writing the awesome Rekkura framework and for dealing with my questions about it. 
I'd also like to thank Mike Genesereth, who teaches the cool General Game Playing class at Stanford. 
Finally, I'd like to give a shoutout to my team from last year (Karl Uhlig and David Goldblatt). 

