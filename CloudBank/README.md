# CloudBank

CloudBank allows CloudCoin Wallet to turn into a server and accept RPC (Remote Procedure Calls) over an SSL connection using a port 
that the owner chooses. 

The main purpose of this is to allow merchants to send and receive funds progomatically. It can also increase the security of an exchange or store
by allowing the coins to be decentralized thus protecting against theft. 

The CloudBank Server service can be accessed by a program on the local computer or by programs accessing remotly over the Internet. If the user wants
to allow remote systems to RPC, the user 
will need to port-forward ports on their local rounter and will need to open ports on their computer's firewall to make it work. 



## Send command
Tells the CloudCoin Wallet to send money from a local wallet to a skywallet. 

SAMPLE COMMAND TO SEND 250 CLOUDCOINS TO sean.cloudcoin.global

```ini
[command]
user=jim
password=ee9df0b377ec434088bcc08b41ae8aaa
command=send

[send]
from_wallet=general
amount=250
To=sean.cloudcoin.global
memo=88773243
wallet_password=a7dc8be5e03548fc8378486b12f3c6a7
```
SAMPLE RESPONSE IF ALL IS GOOD

```ini
memo=88773243
status=AllSent
TotalSend=250
message=All coins send to sean.cloudcoin.global
```
SAMPLE RESPONSE IF ONLY SOME COINS WERE SENT:
```ini
memo=88773243
TotalSend=SomeSent
status=Some
message=Some coins send to sean.cloudcoin.global
```
SAMPLE RESPONSE IF NO COINS WERE SENT:
```ini
memo=88773243
TotalSend=0
status=NoneSent
message=Not enough coins in wallet

```
## Receive Command
This is a command used to check and see if payment was received so that goods and services can be provided. The program will recognize the 
payment based on its memo/tag. 

SAMPLE RECEIVE COMMAND
```ini
[command]
user=jim
password=ee9df0b377ec434088bcc08b41ae8aaa
command=receive

[receive]
to_wallet=general
amount_expected:
skywallet=sean.cloudcoin.global
meme=iiujiieu7er
```

#SAMPLE RESPONSE IF ALL WAS RECEIVED
```int

memo=iiujiieu7er
TotalReceived=250
status=S
message=Some coins send to sean.cloudcoin.global




