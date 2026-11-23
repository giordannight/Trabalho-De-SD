# Servidor da API REST

Servidor HTTP/JSON independente do projeto cliente. Implementa os três objetos distribuídos:

- `HospedagemRemota`
- `ReservaRemota`
- `AutenticacaoRemota`

## Executar

```bat
compilar.bat
executar.bat
```

Ou no PowerShell:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src/*.java | ForEach-Object { $_.FullName })
java -cp out br.ufc.hotelaria.api.ApiServidor 8080
```
