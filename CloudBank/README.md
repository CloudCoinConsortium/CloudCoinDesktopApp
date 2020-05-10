CloudBank
CloudBank allows CloudCoin Wallet to turn into a server and accept RPC (Remote Procedure Calls) over an SSL connection using a port that the owner chooses.
The main purpose of this is to allow merchants to send and receive funds programmatically. It can also increase the security of an exchange or store by allowing the coins to be decentralized, thus protecting against theft.
The CloudBank Server service can be accessed by a program on the local computer or by programs accessing remotely over the Internet. If the user wants to allow remote systems to RPC, the user will need to port-forward ports on their local router and will need to open ports on their computer's firewall to make it work.




## Send command
Tells the CloudCoin Wallet to send money from a local wallet to a skywallet. 

SAMPLE COMMAND TO SEND 250 CLOUDCOINS TO sean.cloudcoin.global

```http
https://localhost/commands/send?user=jim&password=ee9df0b377ec434088bcc08b41ae8aaa&from_wallet=general&amount=250&To=sean.cloudcoin.global&memo=88773243&wallet_password=a7dc8be5e03548fc8378486b12f3c6a7
```
SAMPLE RESPONSE IF ALL IS GOOD

```json
{
	"status": "all_sent",
	"message": "250 coins were sent to sean.cloudcoin.global using memo 88773243",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```
SAMPLE RESPONSE IF ONLY SOME COINS WERE SENT:
```json
{
	"status": "some_sent",
	"message": "150 coins were sent to sean.cloudcoin.global using memo 88773243",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```
SAMPLE RESPONSE IF NO COINS WERE SENT:
```json
{
	"status": "fail",
	"message": "0 coins were sent to sean.cloudcoin.global using memo 88773243",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```

SAMPLE RESPONSE IF MISSING PARAMETERS:
```json
{
	"status": "dud",
	"message": "The parameters $missing was not included or not valid.",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```

## Receive Memo Command
This is a command used to check and see if payment was received so that goods and services can be provided. The program will recognize the 
payment based on its memo/tag. 

SAMPLE RECEIVE COMMAND
```http
https://localhost/commands/receive_memo?user=jim&password=ee9df0b377ec434088bcc08b41ae8aaa&local_wallet=general&local_wallet_password=a7dc8be5e03548fc8378486b12f3c6a7&from_skywallet=sean.cloudcoin.global&memo=for%20purchase%20of%20dollars
```

SAMPLE RESPONSE IF COINS WERE RECEIVED
```json
{
	"status": "received",
	"message": "250 coins were received from sean.cloudcoin.global using memo for purchase of dollars",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```
SAMPLE RESPONSE IF SOME WAS RECEIVED
```json
{
	"status": "received",
	"message": "250 coins were received from sean.cloudcoin.global using memo for purchase of dollars",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```

SAMPLE RESPONSE PARAMETERS WERE MISSING OR NOT VALID
```json
{
	"status": "dud",
	"message": "The parameters $missing was not included or not valid.",
	"time": "yyyy/MM/dd HH:mm:ss",
	"version": "yyyy/MM/dd HH:mm:ss"
}
```

Optimize Change
The optimize change algorithm is run after a "Send" command completes. It must ensure that there is enough change so that the next send command will not be delayed. It could be that the user is able to decide which wallets have changed optimized on. It could be a setting.
This can run one time during the CloudCoin Wallet Startup. Then one time after each send command.
It will check to see that there are:
```
5 ones.

4 fives. 

3 twentyfives.

2 hundreds.
```
If a local wallet does not have this change, it will use the "change" service on the RAIDA to break the largest note. 

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
[response]
memo=88773243
status=AllSent
TotalSend=250
message=All coins send to sean.cloudcoin.global
```
SAMPLE RESPONSE IF ONLY SOME COINS WERE SENT:
```ini
[response]
memo=88773243
TotalSend=SomeSent
status=Some
message=Some coins send to sean.cloudcoin.global
```
SAMPLE RESPONSE IF NO COINS WERE SENT:
```ini
[response]
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
amount_expected=349
skywallet=sean.cloudcoin.global
meme=iiujiieu7er
```

SAMPLE RESPONSE IF ALL WAS RECEIVED
```ini
[response]
memo=iiujiieu7er
TotalReceived=250
status=all received
message=All coins were received
```
SAMPLE RESPONSE IF SOME WAS RECEIVED
```ini
[response]
memo=iiujiieu7er
TotalReceived=349
status=all received
message=Some coins were received
```

SAMPLE RESPONSE IF NONE WAS RECEIVED
```ini
[response]
memo=iiujiieu7er
TotalReceived=0
status=none received
message=No coins were received
```

## Optimize Change
The optimize change algorithm is run after a "Send" command completes. It must ensure that there is enough change so that the next send command will not be delayed. It could be that the user is able to decide which wallets will have changed optimized on. It could be a setting. 
This can run one-time during the CloudCoin Wallet Startup. Then one time after each send command. 

It will check to see that there are: 
```
5 ones.

4 fives. 

3 twenty fives.

2 hundreds.
```








