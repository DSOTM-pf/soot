## 打个先手
引入下面的jar包
  ![](assets/markdown-img-paste-20191227111905656.png)

## 怎么解析？
代码很简单:
  1:给个apk文件的路径。
  2:实例化 ```ProcessManifest```
  3：getPerissions()：获取APP开发过程中使用到的权限
  4：targetSdkVersion：获取targetSdkVersion
```
/**解析AndroidManift.xml*/
public void analysisManifest(String apkPath) throws IOException, XmlPullParserException
{
    //实例化processManifest
    ProcessManifest proManifest = new ProcessManifest(apkPath);
    //获取targetSdkVersion
    System.out.println(proManifest.targetSdkVersion());
    //获取应用在Manifest文件中声明的权限（也包括库声明的，最后一起打包了）
    System.out.println(proManifest.getPermissions());
}
```

## diao用？
先看图

  ![](assets/markdown-img-paste-20191227212748655.png)

获取到的权限：
  - 这里获取到的权限没有区分是否为危险权限，但是此处的权限可以有助于获取应用自身的API-Permission Mapping

targetSdkVersion
  - 重要性不言而喻
