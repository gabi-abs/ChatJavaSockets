Corrigir a versão da linguagem Java no compilador Ainda no Eclipse, botão direito no projeto > Properties

Vá até Java Compiler

Marque a opção "Enable project specific settings"

Em Compiler compliance level, selecione 11

Clique em Apply and Close


Atualizar o Build Path do Projeto
Clique com o botão direito no projeto chatJavaSocket > Build Path > Configure Build Path.

Vá até a aba Libraries.

Remova a entrada com erro: JRE System Library [JavaSE-21].

Clique em Add Library > selecione JRE System Library > clique em Next.

Selecione:

Workspace default JRE (JavaSE-11)
ou

Selecione Alternate JRE e escolha o JDK 11 (se tiver adicionado ele no Eclipse).

Clique em Finish > Apply and Close.
