# EMi client recipe fix

##简介
此mod是EMI的附属插件，只需要在客户端安装。请使用 https://github.com/Pandaismyname1/SEMI 作为前置模组，其他版本的EMI不支持。

##本mod的功能

在Minecraft1.21.2以上的版本中，服务器不再向客户端同步没解锁的配方，这导致JEI，REI，EMI等mod失去了查看配方的功能（哈气了喵）。
本mod实现了在26.2fabric加载器下的配方重建。其原理是：当服务器不向客户端同步配方时，自动读取本地世界的配方来代替。这样查看配方的功能就可以用了。
<img width="1910" height="1021" alt="image" src="https://github.com/user-attachments/assets/23240769-45f4-4afc-825e-8aa6fc93f2cf" />
<img width="1912" height="1008" alt="image" src="https://github.com/user-attachments/assets/e63a1585-3a26-45cd-b111-76aeda378da9" />

##哦对了

本mod100&AI生成，使用deepseek，一共开了2个对话实现了2个功能（作者完全不会代码喵）。一开始AI不知道EMI内部的类，很多可以直接调用EMI的都被AI手动实现了，又繁琐，兼容性又差……
不过后来AI突然开智了，开始调用EMI内部的类，很多之前写的就被弃用了，所以这个mod的代码其实有不少用不到的东西，这也算是历史遗留问题了喵。deepseek真是笨蛋喵。
<img width="1024" height="1536" alt="28174C360A73101F0485FA2A30CFF88C" src="https://github.com/user-attachments/assets/705aabcd-92c5-4a3f-8e93-0f881e45c850" />











## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
