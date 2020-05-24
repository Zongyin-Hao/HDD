# HDD
Static analysis + Dynamic analysis + Active Scheduling  
注：目前仅提供对工程默认包的检测  
  
使用方法：  
1.Eclipse  
（1）Eclipse打开整个项目  
（2）将lib/soot.jar导入Libraries中  
（3）在hybriddetector/Main.java #197中配置工程目录，#198中配置主类，然后运行hybriddetector.Main即可  
  
2.命令行
（1）./build.sh编译项目
（2）./run.sh运行项目，格式为./run.sh classPath mainClassName
