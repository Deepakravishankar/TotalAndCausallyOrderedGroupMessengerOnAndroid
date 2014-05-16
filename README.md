TotalAndCausallyOrderedGroupMessengerOnAndroid
==============================================

Paper:http://dl.acm.org/citation.cfm?id=1066490

Your app should multicast every user-entered message to all app instances (including the one that is sending the message). In the rest of the description, “multicast” always means sending a message to all app instances.

Your app should use B-multicast. It should not implement R-multicast.

You need to come up with an algorithm that provides a total-causal ordering.

Every message should be stored in your provider individually by all app instances. 

Each message should be stored as a <key, value> pair. 

The key should be the final delivery sequence number for the message (as a string); the value should be the actual message (again, as a string).

The delivery sequence number should start from 0 and increase by 1 for each message.
