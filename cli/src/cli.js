import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let serverHost
let serverPort
let previousCommand = "";

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> <serverHost> <serverPort>') 
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    serverHost = args.serverHost
    serverPort = args.serverPort
    server = connect( { host: serverHost, port: serverPort }, () => {    //For local use: { host: 'localhost', port: 8080 }
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    
    server.on('data', (buffer) => {
      let color = 'white';
      let recievedMessage = Message.fromJSON(buffer)

      if(recievedMessage.command === 'echo')        //Changes message color depending on the type of command recieved
        color = 'grey'
      else if(recievedMessage.command === 'connect')
        color = 'green'
      else if(recievedMessage.command === 'disconnect')
        color = 'red'
      else if(recievedMessage.command === 'users')
        color = 'yellow'
      else if(recievedMessage.command === 'broadcast')
        color = 'cyan'
      else if(recievedMessage.command.charAt(0) === '@')
        color = 'magenta'

      this.log(cli.chalk[color](recievedMessage.toString()))
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input,/[^\s]+/g)   //RegEx should ensure that the only thing separating the input is the space character \s                                                       
    const contents = rest.join(' ')                       //Ensures that '@' isn't dropped

    if (command === 'disconnect') 
    {
        server.end(new Message({ username, command }).toJSON() + '\n')
    }
    else if(command === 'users')
    {
        previousCommand = ''
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } 
    else if (command === 'echo') 
    {
        previousCommand = 'echo'
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } 
    else if (command === 'broadcast')
    {
        previousCommand = 'broadcast'
        server.write(new Message({username,command,contents}).toJSON()+'\n')
    }
    else if(command.charAt(0) === '@')  
    {
        previousCommand = command
        server.write(new Message({username,command,contents}).toJSON()+'\n')
    }
    else if(!(previousCommand === ''))  //If no command is specified just repeat the previous command, if it exists
    {
        server.write(new Message({ username, command: previousCommand, contents: command }).toJSON() + '\n')
    }
    else 
    {
        this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
