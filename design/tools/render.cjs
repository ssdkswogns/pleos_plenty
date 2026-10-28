const fs=require('fs'),path=require('path');
let sharp;try{sharp=require('sharp')}catch{sharp=require(require.resolve('sharp',{paths:[process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES]}))}
const root=path.resolve(__dirname,'..');
const manifest=JSON.parse(fs.readFileSync(path.join(root,'screen-manifest.json'),'utf8'));
const asset=fs.readFileSync(path.join(root,'assets/plant-atlas.png')).toString('base64');
async function run(){
 for(const m of manifest){
  const svg=fs.readFileSync(path.join(root,m.svg),'utf8').replaceAll('../assets/plant-atlas.png','data:image/png;base64,'+asset);
  await sharp(Buffer.from(svg)).png().toFile(path.join(root,m.png));
 }
 const badges=path.join(root,'assets/badges');if(fs.existsSync(badges))for(const f of fs.readdirSync(badges).filter(x=>x.endsWith('.svg')))await sharp(path.join(badges,f)).png().toFile(path.join(badges,f.replace('.svg','.png')));
 console.log(`Rendered ${manifest.length} screens`);
}
run().catch(e=>{console.error(e);process.exit(1)});
