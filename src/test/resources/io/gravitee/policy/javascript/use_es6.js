const list = ['a', 'b', 'c'];

context.attributes.length = list.length;
context.attributes.includeA = list.includes('a');

'done';
