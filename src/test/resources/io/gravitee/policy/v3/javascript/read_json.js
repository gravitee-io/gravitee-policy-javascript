var content = JSON.parse(request.content)
content[0].firstname = 'Hacked ' + content[0].firstname
content[0].country = 'US';

JSON.stringify(content);