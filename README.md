interlude
=========

Interlude is intended to be a bulletproof, high-performance, scalable, pluggable backend multiplayer game server. By
leveraging the power of functional programming techniques and the scala actors library, coupled with the popular Netty
networking IO layer, this software provides a superior way to connect multiple clients in a multiplayer environment. The
ultimate goal of Interlude is to provide all of the infrastructure necessary for clients to communicate with the server
(and each other), while providing a pluggable interface that allows the developer to build on and extend the base-level
functionality with their own server-side components.

An example of a fully-functional non-authoritative server (one in which client applications have complete control over
the game logic) is provided out of the box. When I have time, I will add more examples.

Roadmap:

* Retrofit to use akka/spray.io instead of netty/scala-actors
* Introduce spring framework for wiring up components
* Implement an example multiplayer game that uses Interlude
