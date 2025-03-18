# Beach Guard

Beach Guard é um aplicativo desenvolvido em Kotlin que facilita a locação de armários em estabelecimentos como praias, academias, parques, etc. O aplicativo permite que os usuários localizem, reservem e acessem armários de forma rápida e segura, utilizando QR Codes e pulseiras NFC para gerenciar o acesso.

## Funcionalidades

- **Cadastro e Autenticação**: Os usuários podem se cadastrar e fazer login utilizando autenticação via Firebase.
- **Mapa de Armários**: Um mapa interativo mostra todos os armários disponíveis na região do usuário.
- **Locação de Armários**: Os usuários podem selecionar um armário e escolher o tempo de locação.
- **Pagamento e QR Code**: Após a confirmação do pagamento, o usuário recebe um QR Code único para acesso ao armário.
- **Acesso via NFC**: Funcionários do estabelecimento podem escanear o QR Code e gravar a chave de acesso em uma pulseira NFC.
- **Finalização da Locação**: Ao devolver a pulseira NFC, a locação é finalizada e o armário é liberado para o próximo usuário.

## Tecnologias Utilizadas

- **Linguagem**: Kotlin
- **Banco de Dados**: Firebase Firestore
- **Autenticação**: Firebase Authentication
- **Armazenamento de Imagens**: Firebase Storage
- **Geração de QR Code**: Biblioteca ZXing
- **NFC**: Android NFC API