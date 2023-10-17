# IntelliJ Setup

1. At start of IntelliJ, browse to the root `pom.xml` and open it as project.
2. Enable checkstyle:

- Install the [IntelliJ CheckStyle-IDEA Plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea). It can be
  found via plug-in repository  
  (File > Settings > Plugins > Marketplace; **Mac**: IntelliJ IDEA > Preferences > Plugins > Marketplace).  
  ![checkstyle](graphics/checkstyle.PNG)

- Install the CheckStyle-IDEA Plugin, click "Apply" and restart the project upon request.
- Repeat the previous steps for the Lombok Plugin
- Open the Settings (by pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd>; **Mac**: <kbd>
  command</kbd> + <kbd>,</kbd>)
- Go to "Other Settings > Checkstyle".
- Click on "+" under Configuration File and add `checkstyle.xml`. It is located in the root directory. Confirm.

  ![checkstyle](graphics/checkstyle-config.PNG)

- Activate the settings and confirm:

  ![checkstyle](graphics/checkstyle-active.PNG)

3. Configure the code style (Source: <https://youtrack.jetbrains.com/issue/IDEA-61520#comment=27-1292600>)

- Open the Settings (by pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd>; **Mac**: <kbd>
  command</kbd> + <kbd>,</kbd>)
- Go to "Editor > Code Style"
- Click on the gear icon (right of "Scheme:")
- Click "Import Scheme"
- Choose "CheckStyle Configuration"
- Navigate to `checkstyle.xml`. It is located in root directory.
- Click "Apply"
- Click "OK"
- Go to "Editor > Code Style > Java > Imports > Import Layout"
- Adapt the order such that "import java.\*" is before "import javax.*"
- Click "Apply"
- Click "OK"
- Click "Close"

4. Setup code headers to be inserted automatically  
   ![copyright-profile](graphics/copyright-profile-new.png)

- Open the Settings (by pressing <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>S</kbd>; **Mac**: <kbd>
  command</kbd> + <kbd>,</kbd>)
- Go to "Editor > Copyright > Copyright Profiles"
- Click the "+"
- Name "QProv"
- Copyright text from [CodeHeaders](CodeHeaders.md)
- Click "Apply"
- Go to "Editor > Copyright > Formatting"
- Adjust copyright formatting settings

  ![checkstyle](graphics/formatting-copyright-new.png)
    - Change to `Use block comments` with `Prefix each line`
    - Set `Relative Location` to `Before other comments`
    - Set `Separator before`to `80` and `Separator after` to `81`
- Go to "Editor > Copyright"
- Set "QProv" as Default project copyright
- Click "Apply"     
