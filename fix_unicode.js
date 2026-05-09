const fs = require('fs');
const path = require('path');

const replacements = {
    'âœ…': '✅',
    'â Œ': '❌',
    'âš ï¸ ': '⚠️ ',
    'âš\xa0ï¸ ': '⚠️ ',
    'â ³': '⏳',
    'â”€': '─',
    'â€”': '—',
    'â†’': '→',
    'âœ“': '✓',
    'Ã¢â‚¬Â¢': '•'
};

function fixFile(filepath) {
    let content = fs.readFileSync(filepath, 'utf8');
    let newContent = content;
    
    for (const [bad, good] of Object.entries(replacements)) {
        newContent = newContent.split(bad).join(good);
    }
    
    if (newContent !== content) {
        fs.writeFileSync(filepath, newContent, 'utf8');
        console.log('Fixed', filepath);
    }
}

function walk(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filepath = path.join(dir, file);
        if (fs.statSync(filepath).isDirectory()) {
            walk(filepath);
        } else if (filepath.endsWith('.java') || filepath.endsWith('.xml')) {
            fixFile(filepath);
        }
    }
}

walk('d:/Smartwater_chick/app/src/main');
console.log('Done!');
